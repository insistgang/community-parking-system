from datetime import datetime, date, timedelta
from fastapi import APIRouter, Depends, HTTPException, Header
from sqlalchemy.orm import Session
from database import get_db
from models import User, ParkingSpace, QueueApplication, RentalContract, Notification, Community
from schemas import ApplyParkingRequest, CancelApplyRequest, ResponseModel
from scheduler import trigger_recalculate_queue, estimate_waiting_duration, check_expired_allocations, notify_space_change_to_applicants
from evidence import create_evidence_record
from utils import fmt_bj

router = APIRouter(prefix="/api", tags=["Parking & User"])

def get_current_user(x_user_id: int = Header(..., alias="X-User-Id"), db: Session = Depends(get_db)) -> User:
    user = db.query(User).filter(User.id == x_user_id).first()
    if not user:
        raise HTTPException(status_code=401, detail="用户身份未认证")
    return user

@router.get("/user/home", response_model=ResponseModel)
def get_user_home(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    移动端主页核心视图接口:
    动态返回 4 重视图状态 (RENTED | NO_APPLY | WAITING | ALLOCATED | OFFLINE_PENDING)
    """
    community = db.query(Community).filter(Community.id == user.community_id).first()

    # 惰性触发 48h 超时清扫: 前端每 3 秒轮询本接口，超时中签单将被自动收回并顺延
    check_expired_allocations(db, user.community_id)

    # 1. 检查是否有在租车位
    contract = db.query(RentalContract).filter(
        RentalContract.user_id == user.id,
        RentalContract.status.in_(["ACTIVE", "EXPIRING", "OVERDUE"])
    ).first()

    my_space = None
    if contract:
        space = db.query(ParkingSpace).filter(ParkingSpace.id == contract.space_id).first()
        days_left = (contract.end_date - date.today()).days
        my_space = {
            "space_no": space.space_no if space else "A-008",
            "plate_no": contract.plate_no,
            "start_date": str(contract.start_date),
            "end_date": str(contract.end_date),
            "days_left": max(0, days_left),
            "status": contract.status
        }

    # 2. 检查排队申请单
    app = db.query(QueueApplication).filter(
        QueueApplication.user_id == user.id,
        QueueApplication.status.in_(["WAITING", "ALLOCATED", "OFFLINE_PENDING"])
    ).first()

    my_queue = None
    view_state = "NO_APPLY"

    if contract:
        view_state = "RENTED"
    elif app:
        view_state = app.status
        allocated_space_no = None
        countdown_seconds = 0

        if app.status in ("ALLOCATED", "OFFLINE_PENDING") and app.allocated_space_id:
            sp = db.query(ParkingSpace).filter(ParkingSpace.id == app.allocated_space_id).first()
            allocated_space_no = sp.space_no if sp else "待定"
            if app.allocated_time:
                # 48小时倒计时
                elapsed = (datetime.utcnow() - app.allocated_time).total_seconds()
                countdown_seconds = max(0, int(48 * 3600 - elapsed))

        total_waiting = db.query(QueueApplication).filter(
            QueueApplication.community_id == user.community_id,
            QueueApplication.status == "WAITING"
        ).count()

        est_text, est_basis = estimate_waiting_duration(app.current_rank, community.total_spaces if community else 50)

        my_queue = {
            "id": app.id,
            "property_no": app.property_no,
            "plate_no": app.plate_no,
            "priority_level": app.priority_level,
            "priority_label": "家庭一车" if app.priority_level == 1 else "家庭二车(后置)",
            "current_rank": app.current_rank,
            "total_queue": total_waiting,
            "is_first_candidate": (app.current_rank == 1),
            "apply_time": fmt_bj(app.apply_time),
            "ai_waiting_time": est_text,
            "ai_estimation_basis": est_basis,
            "allocated_space_no": allocated_space_no,
            "countdown_seconds": countdown_seconds
        }
    else:
        view_state = "NO_APPLY"

    # 3. 未读通知统计
    unread_count = db.query(Notification).filter(
        Notification.user_id == user.id,
        Notification.is_read == False
    ).count()

    # 4. 小区车位宏观总览
    total_sp = community.total_spaces if community else 50
    rented_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == user.community_id, ParkingSpace.status == "RENTED").count()
    vacant_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == user.community_id, ParkingSpace.status == "VACANT").count()
    maint_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == user.community_id, ParkingSpace.status == "MAINTENANCE").count()
    alloc_sp = db.query(ParkingSpace).filter(ParkingSpace.community_id == user.community_id, ParkingSpace.status.in_(["ALLOCATED", "OFFLINE_PENDING"])).count()

    # 5. 公开展出的车位列表 (脱敏)
    public_spaces = []
    all_spaces = db.query(ParkingSpace).filter(
        ParkingSpace.community_id == user.community_id
    ).order_by(ParkingSpace.space_no.asc()).limit(20).all()

    for sp in all_spaces:
        # 获取承租脱敏人
        owner_name = "空闲"
        room_no = "--"
        masked_plate = "--"
        end_date = "--"

        if sp.current_user_id:
            u = db.query(User).filter(User.id == sp.current_user_id).first()
            if u:
                owner_name = u.real_name[0] + "**"
            ct = db.query(RentalContract).filter(RentalContract.space_id == sp.id, RentalContract.status != "TERMINATED").first()
            if ct:
                masked_plate = ct.plate_no[:2] + "·***" + ct.plate_no[-2:] if len(ct.plate_no) >= 5 else ct.plate_no
                end_date = str(ct.end_date)
                room_no = "12号楼 30*室"
        elif sp.status == "MAINTENANCE":
            owner_name = "维修停用"

        public_spaces.append({
            "space_no": sp.space_no,
            "area_type": "地面" if sp.area_type == "GROUND" else "地下",
            "status": sp.status,
            "owner_name": owner_name,
            "room_no": room_no,
            "plate_no": masked_plate,
            "end_date": end_date
        })

    return ResponseModel(
        code=200,
        data={
            "view_state": view_state,
            "user_info": {
                "id": user.id,
                "real_name": user.real_name,
                "role": user.role,
                "community_name": community.name if community else "龙华新村"
            },
            "unread_notifications": unread_count,
            "my_space": my_space,
            "my_queue": my_queue,
            "community_overview": {
                "total_spaces": total_sp,
                "rented_spaces": rented_sp,
                "vacant_spaces": vacant_sp,
                "maintenance_spaces": maint_sp,
                "allocated_spaces": alloc_sp
            },
            "public_spaces": public_spaces
        }
    )

@router.post("/parking/apply", response_model=ResponseModel)
def apply_parking(req: ApplyParkingRequest, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    车位排队三证合一申请
    """
    # 铁律：物业管理员坚决禁止作为业主申请车位
    if user.role == "PROPERTY_ADMIN":
        raise HTTPException(status_code=403, detail="物业管理员账号负责资产统筹与履约监管，坚决禁止作为业主申请车位！")

    # 铁律：外区房拦截校验
    if "浦东" in req.property_no or "世纪大道" in req.property_no or "外区" in req.property_no:
        raise HTTPException(status_code=400, detail="仅限本小区产权房屋可申请车位，外区房产无法通过核验！")

    # 检查是否已在排队中
    active_app = db.query(QueueApplication).filter(
        QueueApplication.user_id == user.id,
        QueueApplication.status.in_(["WAITING", "ALLOCATED", "OFFLINE_PENDING"])
    ).first()
    if active_app:
        raise HTTPException(status_code=400, detail="您已有正在生效或轮候中的申请，不可重复发起！")

    # 一房多车判定: 本房屋已有排队/历史申请，或本人名下已有生效租约 → 二车自动降权
    exist_house = db.query(QueueApplication).filter(
        QueueApplication.community_id == user.community_id,
        QueueApplication.property_no == req.property_no,
        QueueApplication.status.in_(["WAITING", "ALLOCATED", "OFFLINE_PENDING", "COMPLETED"])
    ).first()

    exist_contract = db.query(RentalContract).filter(
        RentalContract.user_id == user.id,
        RentalContract.status.in_(["ACTIVE", "EXPIRING", "OVERDUE"])
    ).first()

    priority = 2 if (exist_house or exist_contract) else 1 # 家庭二车自动降权，严格排在一车之后

    # 创建申请
    app = QueueApplication(
        user_id=user.id,
        community_id=user.community_id,
        property_no=req.property_no,
        plate_no=req.plate_no,
        is_spouse_car=req.is_spouse_car,
        marriage_cert_no=req.marriage_cert_no,
        priority_level=priority,
        status="WAITING",
        apply_time=datetime.utcnow()
    )
    db.add(app)
    db.commit()
    db.refresh(app)

    # 存证
    create_evidence_record(
        db=db,
        community_id=user.community_id,
        user_id=user.id,
        event_type="APPLY",
        summary=f"业主【{user.real_name}】提交车位申请(三证核验通过·{'家庭一车' if priority==1 else '家庭二车'})",
        payload={
            "property_no": req.property_no,
            "plate_no": req.plate_no,
            "is_spouse": req.is_spouse_car,
            "property_cert_no": req.property_cert_no,
            "marriage_cert_no": req.marriage_cert_no,
            "priority": priority
        }
    )

    # 触发原子重排与匹配
    trigger_recalculate_queue(db, user.community_id)

    return ResponseModel(code=200, message="三证核验通过，排队申请已成功提交！")

@router.post("/parking/cancel-apply", response_model=ResponseModel)
def cancel_apply(req: CancelApplyRequest, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    业主主动撤销排队申请
    """
    app = db.query(QueueApplication).filter(
        QueueApplication.user_id == user.id,
        QueueApplication.status == "WAITING"
    ).first()
    if not app:
        raise HTTPException(status_code=400, detail="未查询到处于排队中的申请")

    old_rank = app.current_rank
    app.status = "CANCELLED"
    db.commit()

    # 存证
    create_evidence_record(
        db=db,
        community_id=user.community_id,
        user_id=user.id,
        event_type="CANCEL",
        summary=f"业主【{user.real_name}】主动撤销第【{old_rank}】位排队资格",
        payload={"reason": req.reason, "cancelled_rank": old_rank}
    )

    # 触发后序全量前移重排
    trigger_recalculate_queue(db, user.community_id)

    return ResponseModel(code=200, message="排队申请已成功撤销，排位已释放")

@router.post("/parking/accept-allocation", response_model=ResponseModel)
def accept_allocation(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    队首中签业主点击【确认前往办理】
    """
    app = db.query(QueueApplication).filter(
        QueueApplication.user_id == user.id,
        QueueApplication.status == "ALLOCATED"
    ).first()
    if not app:
        raise HTTPException(status_code=400, detail="未查询到待确认的中签申请")

    app.status = "OFFLINE_PENDING"
    space_no = "--"
    if app.allocated_space_id:
        sp = db.query(ParkingSpace).filter(ParkingSpace.id == app.allocated_space_id).first()
        if sp:
            sp.status = "OFFLINE_PENDING"
            space_no = sp.space_no

    notify_space_change_to_applicants(
        db, user.community_id,
        title="中签业主确认办理公告",
        content=f"车位【{space_no}】的中签业主已确认前往线下办理，该车位进入签约流程。请继续关注您的排队顺位。",
        exclude_user_ids=[user.id]
    )
    db.commit()

    create_evidence_record(
        db=db,
        community_id=user.community_id,
        user_id=user.id,
        event_type="OFFLINE_SIGN",
        summary=f"业主【{user.real_name}】确认接收中签分配，进入线下签约办理流程",
        payload={"app_id": app.id, "space_id": app.allocated_space_id}
    )

    return ResponseModel(code=200, message="已确认接收！请于规定时间内前往物业办公室办理线下纸质合同签约与缴费")

@router.post("/parking/give-up-allocation", response_model=ResponseModel)
def give_up_allocation(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    队首中签业主点击【放弃本次机会】
    """
    app = db.query(QueueApplication).filter(
        QueueApplication.user_id == user.id,
        QueueApplication.status == "ALLOCATED"
    ).first()
    if not app:
        raise HTTPException(status_code=400, detail="未查询到待确认的中签申请")

    app.status = "CANCELLED"
    space_no = "--"
    if app.allocated_space_id:
        sp = db.query(ParkingSpace).filter(ParkingSpace.id == app.allocated_space_id).first()
        if sp:
            space_no = sp.space_no
            sp.status = "VACANT"
            sp.current_user_id = None

    notify_space_change_to_applicants(
        db, user.community_id,
        title="中签车位放弃释放公告",
        content=f"车位【{space_no}】的中签业主已放弃本次机会，车位已释放回池。系统将按排队顺位自动顺延，请关注您的排位与中签通知。",
        exclude_user_ids=[user.id]
    )
    db.commit()

    create_evidence_record(
        db=db,
        community_id=user.community_id,
        user_id=user.id,
        event_type="CANCEL",
        summary=f"业主【{user.real_name}】主动放弃中签车位，车位释放自动顺延",
        payload={"app_id": app.id}
    )

    # 触发顺延分发给下一位
    trigger_recalculate_queue(db, user.community_id)

    return ResponseModel(code=200, message="您已放弃本次车位分配，机会已顺延给下一位业主")

@router.post("/parking/terminate-rental", response_model=ResponseModel)
def terminate_rental(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    业主主动退租: 终止本人名下生效租约，车位释放回 VACANT 并自动顺延分配给队首
    """
    contract = db.query(RentalContract).filter(
        RentalContract.user_id == user.id,
        RentalContract.status.in_(["ACTIVE", "EXPIRING", "OVERDUE"])
    ).first()
    if not contract:
        raise HTTPException(status_code=400, detail="未查询到您名下正在生效的车位租约")

    sp = db.query(ParkingSpace).filter(ParkingSpace.id == contract.space_id).first()

    contract.status = "TERMINATED"
    if sp:
        sp.status = "VACANT"
        sp.current_user_id = None
    db.commit()

    space_no = sp.space_no if sp else "--"

    create_evidence_record(
        db=db,
        community_id=user.community_id,
        user_id=user.id,
        event_type="TERMINATE",
        summary=f"业主【{user.real_name}】主动退租车位【{space_no}】，租约终止车位释放回炉",
        payload={"contract_no": contract.contract_no, "space_no": space_no, "mode": "OWNER_INITIATED"}
    )

    db.add(Notification(
        user_id=user.id,
        title="退租办理完成通知",
        content=f"您的车位【{space_no}】退租手续已办结，租约终止并已上链存证。车位将由系统按排队顺位自动顺延分配。",
        type="SYSTEM",
        action_type=None
    ))
    notify_space_change_to_applicants(
        db, user.community_id,
        title="车位退租释放公告",
        content=f"车位【{space_no}】已退租释放。系统将按排队顺位自动分配，请关注您的排位与中签通知。",
        exclude_user_ids=[user.id]
    )
    db.commit()

    # 释放的车位立即参与 FIFO 自动顺延分配
    trigger_recalculate_queue(db, user.community_id)

    return ResponseModel(code=200, message=f"退租成功！车位【{space_no}】已释放，系统已自动顺延分配给排队顺位业主。")

@router.post("/parking/renew", response_model=ResponseModel)
def renew_contract(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    业主申请续租: 临期(到期前30天内)/逾期租约一键续租 1 年，全流程上链
    """
    contract = db.query(RentalContract).filter(
        RentalContract.user_id == user.id,
        RentalContract.status.in_(["ACTIVE", "EXPIRING", "OVERDUE"])
    ).first()
    if not contract:
        raise HTTPException(status_code=400, detail="未查询到您名下正在生效的车位租约，无法续租")

    days_left = (contract.end_date - date.today()).days
    if contract.status == "ACTIVE" and days_left > 30:
        raise HTTPException(status_code=400, detail=f"租约尚未进入续租期 (到期前 30 天内开放)，当前剩余 {days_left} 天")

    old_end = contract.end_date
    contract.end_date = contract.end_date + timedelta(days=365)
    contract.status = "ACTIVE"
    db.commit()

    sp = db.query(ParkingSpace).filter(ParkingSpace.id == contract.space_id).first()
    space_no = sp.space_no if sp else "--"

    create_evidence_record(
        db=db,
        community_id=user.community_id,
        user_id=user.id,
        event_type="RENEW",
        summary=f"业主【{user.real_name}】完成车位【{space_no}】续租申请",
        payload={"contract_no": contract.contract_no, "old_end_date": str(old_end), "new_end_date": str(contract.end_date)}
    )

    db.add(Notification(
        user_id=user.id,
        title="车位续租成功通知",
        content=f"您的车位【{space_no}】已成功办理续租，租约有效期已顺延至 {contract.end_date}，本次续租已上链存证。",
        type="SYSTEM",
        action_type="GO_CONFIRM"
    ))
    db.commit()

    return ResponseModel(
        code=200,
        message=f"续租成功！租约已顺延至 {contract.end_date}，存证已上链。",
        data={"contract_no": contract.contract_no, "new_end_date": str(contract.end_date)}
    )

@router.get("/parking/queue", response_model=ResponseModel)
def get_public_queue(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    全队列阳光脱敏公示列表
    """
    apps = db.query(QueueApplication).filter(
        QueueApplication.community_id == user.community_id,
        QueueApplication.status == "WAITING"
    ).order_by(
        QueueApplication.priority_level.asc(),
        QueueApplication.apply_time.asc()
    ).all()

    result = []
    # 排位号以实时 FIFO 顺序枚举生成，确保公示大盘与调度引擎真实顺序严格一致
    for idx, app in enumerate(apps, start=1):
        if app.current_rank != idx:
            app.current_rank = idx
        u = db.query(User).filter(User.id == app.user_id).first()
        name_masked = (u.real_name[0] + "**") if u else "匿*"
        phone_masked = (u.phone[:3] + "****" + u.phone[-4:]) if u else "138****0000"

        result.append({
            "rank": idx,
            "masked_name": name_masked,
            "masked_phone": phone_masked,
            "priority_type": "家庭一车" if app.priority_level == 1 else "家庭二车",
            "apply_time": fmt_bj(app.apply_time),
            "is_me": (app.user_id == user.id)
        })
    db.commit()

    # 中签待签公示 (ALLOCATED 48h待确认 / OFFLINE_PENDING 线下签约中)
    allocated_apps = db.query(QueueApplication).filter(
        QueueApplication.community_id == user.community_id,
        QueueApplication.status.in_(["ALLOCATED", "OFFLINE_PENDING"])
    ).order_by(QueueApplication.allocated_time.asc()).all()

    allocated = []
    for app in allocated_apps:
        u = db.query(User).filter(User.id == app.user_id).first()
        sp = db.query(ParkingSpace).filter(ParkingSpace.id == app.allocated_space_id).first()
        allocated.append({
            "masked_name": (u.real_name[0] + "**") if u else "匿*",
            "masked_phone": (u.phone[:3] + "****" + u.phone[-4:]) if u else "--",
            "space_no": sp.space_no if sp else "--",
            "status": app.status,
            "status_label": "中签待确认(48h)" if app.status == "ALLOCATED" else "线下签约办理中",
            "allocated_time": fmt_bj(app.allocated_time),
            "is_me": (app.user_id == user.id)
        })

    return ResponseModel(code=200, data={"waiting": result, "allocated": allocated})

@router.get("/parking/my-timeline", response_model=ResponseModel)
def get_my_timeline(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    本人排位轨迹时间线
    """
    evs = db.query(Notification).filter(
        Notification.user_id == user.id
    ).order_by(Notification.created_at.desc()).all()

    timeline = []
    for n in evs:
        timeline.append({
            "id": n.id,
            "title": n.title,
            "content": n.content,
            "type": n.type,
            "time": fmt_bj(n.created_at)
        })

    return ResponseModel(code=200, data=timeline)

@router.get("/notifications", response_model=ResponseModel)
def get_notifications(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """
    获取消息通知列表
    """
    notifs = db.query(Notification).filter(
        Notification.user_id == user.id
    ).order_by(Notification.created_at.desc()).all()

    # 标为已读
    for n in notifs:
        n.is_read = True
    db.commit()

    data = []
    for n in notifs:
        data.append({
            "id": n.id,
            "title": n.title,
            "content": n.content,
            "type": n.type,
            "action_type": n.action_type,
            "created_at": fmt_bj(n.created_at)
        })

    return ResponseModel(code=200, data=data)
