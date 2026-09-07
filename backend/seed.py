import hashlib
from datetime import datetime, date, timedelta
from database import engine, SessionLocal, Base
from models import Community, User, ParkingSpace, QueueApplication, RentalContract, ChainEvidence, Notification
from evidence import create_evidence_record

def init_db_and_seed():
    # 重新构建所有表
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()
    try:
        # 1. 创建小区
        community = Community(
            name="某某新村",
            district="徐汇区",
            subdistrict="湖南路街道",
            total_spaces=50
        )
        db.add(community)
        db.commit()
        db.refresh(community)

        # 密码哈希辅助
        def make_pwd(pwd="123456"):
            return hashlib.sha256(pwd.encode('utf-8')).hexdigest()

        # 2. 创建四大演示账号
        # (1) 张欣欣: 未申请新客
        user_zhang = User(
            phone="13800000001",
            id_card="310104199201011111",
            password_hash=make_pwd("123456"),
            real_name="张欣欣",
            role="OWNER",
            community_id=community.id
        )
        # (2) 李雷: 队首中签者 (待确认48h)
        user_li = User(
            phone="13800000002",
            id_card="310104199002022222",
            password_hash=make_pwd("123456"),
            real_name="李雷",
            role="OWNER",
            community_id=community.id
        )
        # (3) 韩梅梅: 已有车位业主 (剩余25天临期)
        user_han = User(
            phone="13800000003",
            id_card="310104199103033333",
            password_hash=make_pwd("123456"),
            real_name="韩梅梅",
            role="OWNER",
            community_id=community.id
        )
        # (4) 王建国: 物业管理员
        user_wang = User(
            phone="13900000001",
            id_card="310104198005055555",
            password_hash=make_pwd("123456"),
            real_name="王建国",
            role="PROPERTY_ADMIN",
            community_id=community.id
        )
        
        # (5) 两个排在前序的虚拟业主 (赵强排第1, 钱小敏排第2, 确保张欣欣申请后排第3)
        user_zhao = User(
            phone="13800000004",
            id_card="310104198804044444",
            password_hash=make_pwd("123456"),
            real_name="赵强",
            role="OWNER",
            community_id=community.id
        )
        user_qian = User(
            phone="13800000005",
            id_card="310104198905055555",
            password_hash=make_pwd("123456"),
            real_name="钱小敏",
            role="OWNER",
            community_id=community.id
        )
        
        # 虚拟在租业主代表 (用于被清退)
        user_sun = User(
            phone="13800000006",
            id_card="310104197506066666",
            password_hash=make_pwd("123456"),
            real_name="孙立新",
            role="OWNER",
            community_id=community.id
        )

        db.add_all([user_zhang, user_li, user_han, user_wang, user_zhao, user_qian, user_sun])
        db.commit()

        # 3. 创建 50 个车位资产 (A-001 ~ A-050)
        # 初始全部占满: 48个RENTED, 1个ALLOCATED(A-012), 1个MAINTENANCE(A-050), 0个VACANT
        spaces = []
        for i in range(1, 51):
            space_no = f"A-{i:03d}"
            status = "RENTED"
            curr_user_id = None
            if i == 8:
                curr_user_id = user_han.id # 韩梅梅车位 A-008
            elif i == 12:
                status = "ALLOCATED"
                curr_user_id = user_li.id # 李雷中签车位 A-012
            elif i == 15:
                curr_user_id = user_sun.id # 孙立新占用车位 A-015 (用于演示物业清退)
            elif i == 50:
                status = "MAINTENANCE" # 维修停用车位
            else:
                curr_user_id = None # 其余虚拟已租

            sp = ParkingSpace(
                community_id=community.id,
                space_no=space_no,
                area_type="GROUND" if i <= 30 else "UNDERGROUND",
                status=status,
                current_user_id=curr_user_id
            )
            spaces.append(sp)
        db.add_all(spaces)
        db.commit()

        # 4. 创建在租合同
        # 韩梅梅 (剩余 25 天)
        today = date.today()
        c_han = RentalContract(
            contract_no="AGY-CT-20251001-0008",
            space_id=spaces[7].id, # A-008
            user_id=user_han.id,
            plate_no="沪A·88888",
            start_date=today - timedelta(days=340),
            end_date=today + timedelta(days=25),
            status="EXPIRING"
        )
        # 孙立新 (已逾期/卖房失联，用于演示物业强制代清退)
        c_sun = RentalContract(
            contract_no="AGY-CT-20250801-0015",
            space_id=spaces[14].id, # A-015
            user_id=user_sun.id,
            plate_no="沪B·99999",
            start_date=today - timedelta(days=370),
            end_date=today - timedelta(days=5),
            status="OVERDUE"
        )
        db.add_all([c_han, c_sun])
        db.commit()

        # 5. 创建排队队列初始数据
        # 统一使用 UTC 时间，与 scheduler.py / parking.py 的 48h 倒计时计算保持一致
        now = datetime.utcnow()
        # 李雷: 已处于 ALLOCATED 待确认 (分配了 A-012，倒计时起始点设为 10 分钟前)
        app_li = QueueApplication(
            user_id=user_li.id,
            community_id=community.id,
            property_no="8号楼201室",
            plate_no="沪A·22222",
            is_spouse_car=False,
            priority_level=1,
            current_rank=0,
            status="ALLOCATED",
            allocated_space_id=spaces[11].id, # A-012
            allocated_time=now - timedelta(minutes=10),
            apply_time=now - timedelta(days=10)
        )
        # 赵强: 排在第 1 位
        app_zhao = QueueApplication(
            user_id=user_zhao.id,
            community_id=community.id,
            property_no="9号楼102室",
            plate_no="沪A·44444",
            is_spouse_car=False,
            priority_level=1,
            current_rank=1,
            status="WAITING",
            apply_time=now - timedelta(days=5)
        )
        # 钱小敏: 排在第 2 位
        app_qian = QueueApplication(
            user_id=user_qian.id,
            community_id=community.id,
            property_no="10号楼403室",
            plate_no="沪A·55555",
            is_spouse_car=False,
            priority_level=1,
            current_rank=2,
            status="WAITING",
            apply_time=now - timedelta(days=3)
        )
        db.add_all([app_li, app_zhao, app_qian])
        db.commit()

        # 6. 初始化初始存证记录
        create_evidence_record(
            db=db,
            community_id=community.id,
            user_id=user_wang.id,
            event_type="INIT",
            summary="物业管理员王建国完成【某某新村】50个车位台账初始化导入锁定",
            payload={"total_spaces": 50, "manager": "王建国", "community": "某某新村"}
        )

        create_evidence_record(
            db=db,
            community_id=community.id,
            user_id=user_li.id,
            event_type="ALLOCATE",
            summary="系统自动分配车位【A-012】给李雷 (启动48小时倒计时)",
            payload={"space_no": "A-012", "user": "李雷"}
        )

        # 7. 李雷的 48h 中签通知
        db.add(Notification(
            user_id=user_li.id,
            title="车位中签锁定提醒",
            content="恭喜！系统已按排队顺位为您锁定车位【A-012】，请在 48 小时内确认前往物业办公室办理线下签约！",
            type="ALLOCATION",
            action_type="GO_CONFIRM"
        ))

        # 韩梅梅的临期催缴通知
        db.add(Notification(
            user_id=user_han.id,
            title="车位租约即将到期提醒",
            content="您的车位【A-008】租约剩余 25 天到期，系统已开放续租入口，请及时前往物业办理续签以防逾期自动清退！",
            type="EXPIRATION",
            action_type="GO_CONFIRM"
        ))

        db.commit()
        print(">>> 演示数据矩阵 (Seed Data) 精准初始化完成！")

    finally:
        db.close()

if __name__ == "__main__":
    init_db_and_seed()
