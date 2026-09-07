from datetime import datetime, date, timedelta
from fastapi import APIRouter, Depends, HTTPException, Header
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
import urllib.parse
from io import BytesIO
from database import get_db
from models import User, ParkingSpace, QueueApplication, RentalContract, Notification, Community
from schemas import MaintenanceRequest, ConfirmDeliveryRequest, ForceTerminateRequest, ResponseModel
from scheduler import trigger_recalculate_queue, init_community_spaces
from evidence import create_evidence_record
from utils import fmt_bj

router = APIRouter(prefix="/api/property", tags=["Property Management"])

def get_property_admin(x_user_id: int = Header(..., alias="X-User-Id"), db: Session = Depends(get_db)) -> User:
    user = db.query(User).filter(User.id == x_user_id).first()
    if not user or user.role != "PROPERTY_ADMIN":
        raise HTTPException(status_code=403, detail="仅限物业管理员权限访问")
    return user

@router.get("/stats", response_model=ResponseModel)
def get_property_stats(admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    物业 PC 宽屏数据看板指标
    """
    comm_id = admin.community_id
    total_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == comm_id).count()
    rented_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == comm_id, ParkingSpace.status == "RENTED").count()
    vacant_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == comm_id, ParkingSpace.status == "VACANT").count()
    maint_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == comm_id, ParkingSpace.status == "MAINTENANCE").count()
    allocated_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == comm_id, ParkingSpace.status.in_(["ALLOCATED", "OFFLINE_PENDING"])).count()
    waiting_users = db.query(QueueApplication).filter(QueueApplication.community_id == comm_id, QueueApplication.status == "WAITING").count()

    # 待办预警数 (即将到期 + 待确认中签)，严格限定本小区范围
    expiring_count = db.query(RentalContract).join(
        ParkingSpace, RentalContract.space_id == ParkingSpace.id
    ).filter(ParkingSpace.community_id == comm_id, RentalContract.status == "EXPIRING").count()
    pending_delivery = db.query(QueueApplication).filter(
        QueueApplication.community_id == comm_id,
        QueueApplication.status == "OFFLINE_PENDING"
    ).count()

    return ResponseModel(
        code=200,
        data={
            "total_spaces": total_sp,
            "rented_spaces": rented_sp,
            "vacant_spaces": vacant_sp,
            "maintenance_spaces": maint_sp,
            "allocated_spaces": allocated_sp,
            "waiting_count": waiting_users,
            "occupancy_rate": f"{(rented_sp / max(1, total_sp) * 100):.1f}%",
            "pending_warnings": expiring_count + pending_delivery,
            "avg_turnover_days": 85
        }
    )

@router.get("/spaces", response_model=ResponseModel)
def get_property_spaces(admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    车位全生命周期台账网格
    """
    spaces = db.query(ParkingSpace).filter(
        ParkingSpace.community_id == admin.community_id
    ).order_by(ParkingSpace.space_no.asc()).all()

    result = []
    for sp in spaces:
        user_name = "--"
        user_phone = "--"
        plate_no = "--"
        end_date = "--"

        if sp.current_user_id:
            u = db.query(User).filter(User.id == sp.current_user_id).first()
            if u:
                user_name = u.real_name
                user_phone = u.phone
            ct = db.query(RentalContract).filter(RentalContract.space_id == sp.id, RentalContract.status != "TERMINATED").first()
            if ct:
                plate_no = ct.plate_no
                end_date = str(ct.end_date)
            else:
                app = db.query(QueueApplication).filter(QueueApplication.allocated_space_id == sp.id).first()
                if app:
                    plate_no = app.plate_no
                    end_date = "48h待签约"

        result.append({
            "id": sp.id,
            "space_no": sp.space_no,
            "area_type": sp.area_type,
            "status": sp.status,
            "user_id": sp.current_user_id,
            "user_name": user_name,
            "user_phone": user_phone,
            "plate_no": plate_no,
            "end_date": end_date
        })

    return ResponseModel(code=200, data=result)

@router.post("/set-maintenance", response_model=ResponseModel)
def set_maintenance(req: MaintenanceRequest, admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    车位维修停用 / 恢复可用
    """
    sp = db.query(ParkingSpace).filter(ParkingSpace.id == req.space_id).first()
    if not sp:
        raise HTTPException(status_code=404, detail="未找到指定车位")

    if req.action == "SET_MAINTENANCE":
        if sp.status in ("RENTED", "ALLOCATED", "OFFLINE_PENDING"):
            raise HTTPException(status_code=400, detail="该车位当前已在使用或处于分配确认中，不可置为维修！")
        sp.status = "MAINTENANCE"
        msg = f"车位【{sp.space_no}】已置为维修停用，已移出自动排位池"
    elif req.action == "RESTORE":
        sp.status = "VACANT"
        msg = f"车位【{sp.space_no}】已恢复可用，将自动参与顺位分配"
    else:
        raise HTTPException(status_code=400, detail="无效的操作类型")

    db.commit()

    # 若恢复可用，触发排位自动检测
    if req.action == "RESTORE":
        trigger_recalculate_queue(db, admin.community_id)

    create_evidence_record(
        db=db,
        community_id=admin.community_id,
        user_id=admin.id,
        event_type="MAINTENANCE",
        summary=f"物业管理员操作车位【{sp.space_no}】：{'维修停用' if req.action=='SET_MAINTENANCE' else '恢复可用'}",
        payload={"space_no": sp.space_no, "action": req.action}
    )

    return ResponseModel(code=200, message=msg)

@router.post("/confirm-delivery", response_model=ResponseModel)
def confirm_delivery(req: ConfirmDeliveryRequest, admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    物业线下签约完成交付:
    将车位转为 RENTED，生成正式 1 年期租约与存证
    """
    sp = db.query(ParkingSpace).filter(ParkingSpace.id == req.space_id).first()
    u = db.query(User).filter(User.id == req.user_id).first()
    if not sp or not u:
        raise HTTPException(status_code=404, detail="车位或业主不存在")

    # 防插队铁律: 仅允许交付"系统已自动分配给该业主"的车位，坚决禁止任何手动指派后门
    if sp.status not in ("ALLOCATED", "OFFLINE_PENDING") or sp.current_user_id != u.id:
        raise HTTPException(status_code=400, detail="该车位当前并非分配给此业主的中签车位，禁止人工指定交付！车位只能由系统按排队顺位自动分发。")

    app = db.query(QueueApplication).filter(
        QueueApplication.user_id == u.id,
        QueueApplication.allocated_space_id == sp.id,
        QueueApplication.status.in_(["ALLOCATED", "OFFLINE_PENDING"])
    ).first()
    if not app:
        raise HTTPException(status_code=400, detail="未找到该业主对应该车位的中签申请单，无法交付")

    # 防止同一车位重复生成交付合同
    exist_ct = db.query(RentalContract).filter(
        RentalContract.space_id == sp.id,
        RentalContract.status != "TERMINATED"
    ).first()
    if exist_ct:
        raise HTTPException(status_code=400, detail="该车位已存在生效租约，不可重复交付")

    sp.status = "RENTED"
    sp.current_user_id = u.id
    app.status = "COMPLETED"

    # 生成正式租约
    today = date.today()
    contract_no = f"AGY-CT-{today.strftime('%Y%m%d')}-{sp.space_no.replace('-', '')}"
    contract = RentalContract(
        contract_no=contract_no,
        space_id=sp.id,
        user_id=u.id,
        plate_no=app.plate_no if app else "沪A·88888",
        start_date=today,
        end_date=today + timedelta(days=365),
        status="ACTIVE"
    )
    db.add(contract)
    db.commit()

    # 存证
    create_evidence_record(
        db=db,
        community_id=admin.community_id,
        user_id=u.id,
        event_type="SIGN",
        summary=f"物业与业主【{u.real_name}】完成车位【{sp.space_no}】线下签约履约交付",
        payload={"contract_no": contract_no, "space_no": sp.space_no, "term": "1年"}
    )

    db.add(Notification(
        user_id=u.id,
        title="车位签约成功生效通知",
        content=f"恭喜！您与物业已完成车位【{sp.space_no}】的线下签约缴费，租期 1 年已正式生效！",
        type="SYSTEM",
        action_type="GO_CONFIRM"
    ))
    db.commit()

    return ResponseModel(code=200, message=f"车位【{sp.space_no}】已成功交付业主【{u.real_name}】，正式租约已生效！")

@router.post("/force-terminate", response_model=ResponseModel)
def force_terminate(req: ForceTerminateRequest, admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    物业代发起退租 / 强制清退异常占用:
    释放车位回 VACANT，触发调度引擎，后序顺位秒级前移！
    """
    sp = db.query(ParkingSpace).filter(ParkingSpace.id == req.space_id).first()
    if not sp:
        raise HTTPException(status_code=404, detail="未找到指定车位")
    if sp.status in ("VACANT", "MAINTENANCE"):
        raise HTTPException(status_code=400, detail="该车位当前未被租用占用，无需清退")

    old_user_id = sp.current_user_id
    old_user_name = "老业主"
    if old_user_id:
        ou = db.query(User).filter(User.id == old_user_id).first()
        if ou:
            old_user_name = ou.real_name

    # 若车位处于中签确认期 (ALLOCATED/OFFLINE_PENDING)，同步作废关联申请单，杜绝一车位两申请单的数据错乱
    pending_app = db.query(QueueApplication).filter(
        QueueApplication.allocated_space_id == sp.id,
        QueueApplication.status.in_(["ALLOCATED", "OFFLINE_PENDING"])
    ).first()
    if pending_app:
        pending_app.status = "CANCELLED"
        pending_app.allocated_space_id = None
        pending_app.allocated_time = None
        db.add(Notification(
            user_id=pending_app.user_id,
            title="中签分配撤销通知",
            content=f"很抱歉，您中签的车位【{sp.space_no}】因物业原因({req.reason})被收回，本次分配已作废。如需车位请重新提交排队申请。",
            type="SYSTEM",
            action_type="GO_QUEUE"
        ))

    # 终止旧合同
    old_ct = db.query(RentalContract).filter(
        RentalContract.space_id == sp.id,
        RentalContract.status != "TERMINATED"
    ).first()
    if old_ct:
        old_ct.status = "TERMINATED"

    # 释放车位为 VACANT
    sp.status = "VACANT"
    sp.current_user_id = None
    db.commit()

    # 全流程上链存证 (带清退原因和凭据)
    create_evidence_record(
        db=db,
        community_id=admin.community_id,
        user_id=old_user_id,
        event_type="TERMINATE",
        summary=f"物业代清退车位【{sp.space_no}】(原业主:{old_user_name}, 事由:{req.reason})",
        payload={
            "space_no": sp.space_no,
            "old_user": old_user_name,
            "reason": req.reason,
            "attachment": req.attachment_url
        }
    )

    # 核心联动：触发自动重排与顺延分配，后序顺位秒级前移！
    trigger_recalculate_queue(db, admin.community_id)

    return ResponseModel(code=200, message=f"车位【{sp.space_no}】已成功清退释放，系统已自动触发后序顺位流转分配！")

@router.get("/export-spaces")
def export_spaces(admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    导出本小区车位资产台账为真实 Excel (.xlsx) 文件:
    Sheet1 车位资产台账 + Sheet2 排队轮候名单
    """
    from openpyxl import Workbook
    from openpyxl.styles import Font, PatternFill, Alignment

    community = db.query(Community).filter(Community.id == admin.community_id).first()
    community_name = community.name if community else "小区"

    status_map = {
        "RENTED": "已租(生效中)",
        "VACANT": "空闲可用",
        "ALLOCATED": "已分配待确认(48h)",
        "OFFLINE_PENDING": "线下签约办理中",
        "MAINTENANCE": "维修停用"
    }

    wb = Workbook()
    header_font = Font(bold=True, color="FFFFFF")
    header_fill = PatternFill("solid", fgColor="1D4ED8")
    center = Alignment(horizontal="center", vertical="center")

    def style_header(ws):
        for c in ws[1]:
            c.font = header_font
            c.fill = header_fill
            c.alignment = center

    # Sheet1: 车位资产台账
    ws = wb.active
    ws.title = "车位资产台账"
    ws.append(["车位编号", "区域类型", "车位状态", "关联业主", "联系电话", "登记车牌", "租约编号", "租期截止日"])
    style_header(ws)

    spaces = db.query(ParkingSpace).filter(
        ParkingSpace.community_id == admin.community_id
    ).order_by(ParkingSpace.space_no.asc()).all()

    for sp in spaces:
        user_name, user_phone, plate_no, contract_no, end_date = "--", "--", "--", "--", "--"
        if sp.current_user_id:
            u = db.query(User).filter(User.id == sp.current_user_id).first()
            if u:
                user_name, user_phone = u.real_name, u.phone
            ct = db.query(RentalContract).filter(
                RentalContract.space_id == sp.id,
                RentalContract.status != "TERMINATED"
            ).first()
            if ct:
                plate_no, contract_no, end_date = ct.plate_no, ct.contract_no, str(ct.end_date)
            else:
                app = db.query(QueueApplication).filter(
                    QueueApplication.allocated_space_id == sp.id,
                    QueueApplication.status.in_(["ALLOCATED", "OFFLINE_PENDING"])
                ).first()
                if app:
                    plate_no, end_date = app.plate_no, "48h待签约"
        ws.append([
            sp.space_no,
            "地面" if sp.area_type == "GROUND" else "地下",
            status_map.get(sp.status, sp.status),
            user_name, user_phone, plate_no, contract_no, end_date
        ])

    # Sheet2: 排队轮候名单
    ws2 = wb.create_sheet("排队轮候名单")
    ws2.append(["实时排位", "业主姓名", "联系电话", "登记车牌", "优先级", "申报房屋", "申请时间"])
    style_header(ws2)

    waiting_apps = db.query(QueueApplication).filter(
        QueueApplication.community_id == admin.community_id,
        QueueApplication.status == "WAITING"
    ).order_by(QueueApplication.priority_level.asc(), QueueApplication.apply_time.asc()).all()

    for idx, app in enumerate(waiting_apps, start=1):
        u = db.query(User).filter(User.id == app.user_id).first()
        ws2.append([
            idx,
            u.real_name if u else "--",
            u.phone if u else "--",
            app.plate_no,
            "家庭一车" if app.priority_level == 1 else "家庭二车",
            app.property_no,
            fmt_bj(app.apply_time)
        ])

    for sheet in (ws, ws2):
        for col in sheet.columns:
            max_len = max((len(str(c.value)) for c in col if c.value is not None), default=10)
            sheet.column_dimensions[col[0].column_letter].width = min(32, max(12, max_len * 2 + 4))

    buf = BytesIO()
    wb.save(buf)
    buf.seek(0)

    filename = f"{community_name}_车位资产台账_{date.today().strftime('%Y%m%d')}.xlsx"
    return StreamingResponse(
        buf,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f"attachment; filename*=UTF-8''{urllib.parse.quote(filename)}"}
    )

@router.post("/init-demo-spaces", response_model=ResponseModel)
def init_demo_spaces(admin: User = Depends(get_property_admin), db: Session = Depends(get_db)):
    """
    一键注入 50 个车位演示数据 (仅限当前小区尚无车位资产时)
    """
    exist = db.query(ParkingSpace).filter(ParkingSpace.community_id == admin.community_id).count()
    if exist > 0:
        raise HTTPException(status_code=400, detail="当前小区已存在车位台账，无需重复初始化")

    spaces = init_community_spaces(db, admin.community_id, 50)

    community = db.query(Community).filter(Community.id == admin.community_id).first()
    if community:
        community.total_spaces = 50
        db.commit()

    create_evidence_record(
        db=db,
        community_id=admin.community_id,
        user_id=admin.id,
        event_type="INIT",
        summary=f"物业管理员【{admin.real_name}】完成本小区 50 个车位台账初始化导入锁定",
        payload={"total_spaces": 50, "manager": admin.real_name}
    )

    first_no = spaces[0].space_no if spaces else "--"
    last_no = spaces[-1].space_no if spaces else "--"
    return ResponseModel(code=200, message=f"已成功注入 50 个车位资产 ({first_no} ~ {last_no})，台账已上链锁定！")
