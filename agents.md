# 老旧小区停车位管理系统 Agent 开发工程指令与架构规范 (AGENTS.md)

> **最高工程纲领**：本文件为 AI Coding Agent 执行《老旧小区停车位管理系统产品需求规格说明书 (PRD-V3.0)》代码构建的**终极工程宪法与研发落地规范**。后续所有代码编写、模块拆解、API 契约实现、数据建模与前端页面交互必须 100% 严格对标本指南，**严禁擅自删减业务规则、遗漏异常拦截或变更技术栈**。
> 
> **核心里程碑目标**：**2026-09-08 (明天周二) 下班前交付前后端全栈可交互、零外部依赖、自包含、高颜值的演示系统；2026-09-09 (周三) 下午供业务负责人向张总汇报评审**。

---

## 目录
1. [技术栈选型与工程拓扑架构](#1-技术栈选型与工程拓扑架构)
2. [不可逾越的六大业务与合规红线](#2-不可逾越的六大业务与合规红线)
3. [数据库实体模型 (SQLite DDL & SQLAlchemy 2.0)](#3-数据库实体模型-sqlite-ddl--sqlalchemy-20)
4. [全量 RESTful API 契约与 JSON Schema 规范](#4-全量-restful-api-契约与-json-schema-规范)
5. [调度引擎核心逻辑与原子重排算法 (scheduler.py)](#5-调度引擎核心逻辑与原子重排算法-schedulerpy)
6. [虚拟区块链存证服务与防伪凭据算法 (evidence.py)](#6-虚拟区块链存证服务与防伪凭据算法-evidencepy)
7. [前端 Vue 3 + Tailwind CSS 界面与动态视图设计](#7-前端-vue-3--tailwind-css-界面与动态视图设计)
8. [开箱即用演示预置数据矩阵与一键重置机制 (seed.py)](#8-开箱即用演示预置数据矩阵与一键重置机制-seedpy)
9. [周三向张总汇报：经典四幕演示链路自动化保障](#9-周三向张总汇报经典四幕演示链路自动化保障)
10. [工程一键启动、本地离线化运行与交付自测标准](#10-工程一键启动本地离线化运行与交付自测标准)

---

## 1. 技术栈选型与工程拓扑架构

### 1.1 技术栈基准 (Technology Stack)
- **后端架构**：`Python 3.10+` + `FastAPI` (极速异步 RESTful 服务，自动集成 Swagger `/docs`)
- **数据持久化**：`SQLite 3` + `SQLAlchemy 2.0` (零配置单文件数据库 `parking.db`)
- **前端架构**：`Vue 3` (Composition API + `<script setup>`) + `Vite` + `Tailwind CSS 3.x` + `Pinia` (全局状态) + `Vue Router 4`
- **UI 组件与动效**：内嵌 SVG 矢量图标 (无外链 CDN 依赖) + 科技蓝/翡翠绿调色盘 + CSS 拟真扫描识别与倒计时动画
- **环境要求**：**纯离线可运行 (Zero External CDN Dependency)**，所有资源本地打包，确保在会议室无外网环境下 100% 毫秒级秒开。

### 1.2 标准工程目录拓扑规范
```text
/Users/insistgang/Desktop/test/
├── agents.md                                     # 本工程开发宪法 (V3.0 终极版)
├── 老旧小区停车位管理系统_开发PRD文档_v3.0.md   # 产品需求终审基线
├── run.sh                                        # 全栈一键启动与依赖初始化脚本
├── backend/                                      # 后端 Python 服务目录
│   ├── main.py                                   # FastAPI 主入口与 CORS/异常处理
│   ├── database.py                               # SQLite 连接引擎与 SessionLocal
│   ├── models.py                                 # SQLAlchemy ORM 6张核心数据实体
│   ├── schemas.py                                # Pydantic 请求与响应序列化契约
│   ├── scheduler.py                              # FIFO 自动排位与原子重排引擎
│   ├── evidence.py                               # 虚拟区块链存证服务 (SHA-256)
│   ├── seed.py                                   # 演示数据初始化与重置脚本
│   ├── requirements.txt                          # 后端依赖清单
│   └── routers/
│       ├── auth.py                               # 注册与双通道登录
│       ├── parking.py                            # 申请、撤销、顺位公示、时间线
│       ├── property.py                           # 物业资产、维修停用、清退审核
│       ├── evidence.py                           # 个人区块链存证查询与验真凭据
│       └── dev.py                                # 一键重置演示数据专有接口
└── frontend/                                     # 前端 Vue 3 工程目录
    ├── index.html                                # 单页应用入口
    ├── package.json                              # 前端依赖配置
    ├── vite.config.js                            # Vite 开发与构建代理配置
    ├── tailwind.config.js                        # Tailwind CSS 主题配置
    └── src/
        ├── main.js                               # Vue 3 入口
        ├── App.vue                               # 根组件 (含全局消息与弹窗宿主)
        ├── router/index.js                       # 路由守卫与双端路由
        ├── store/                                # Pinia 状态仓库 (userStore, queueStore)
        ├── api/                                  # Axios API 模块化封装
        ├── components/                           # 高复用业务组件
        │   ├── NavBar.vue                        # 顶部导航栏 (小区名+通知铃铛红点)
        │   ├── SeedAccountBar.vue                # 顶部快捷填充测试账号条
        │   ├── CountdownCard.vue                 # 48h 动态高亮倒计时卡片
        │   ├── EvidenceModal.vue                 # 区块链数字存证证书防伪弹窗
        │   ├── OcrUploadBox.vue                  # 身份证/结婚证 1秒拟真扫描组件
        │   └── AiWaitingCard.vue                 # 大模型预估等待时长与算法展开卡
        └── views/
            ├── LoginView.vue                     # 真实双通道登录页 (带一键快捷填充)
            ├── RegisterView.vue                  # 三级区划选择注册页
            ├── HomeView.vue                      # 业主移动端首页 (动态4重视图+宏观概况)
            ├── ApplyParkingView.vue              # 三证合一申请页 (外区房拦截+夫妻车引导)
            ├── PublicQueueView.vue               # 顺位全队列脱敏公示页 (带个人时间线)
            ├── EvidenceCenterView.vue            # 个人区块链存证中心
            ├── NotificationsView.vue             # 消息通知中心 (顺位前移/多级催缴)
            └── property/
                ├── PropertyLoginView.vue         # 物业营业执照上传与法人认证
                └── PropertyDashboardView.vue     # 物业 PC 宽屏中台 (大屏/台账/清退)
```

---

## 2. 不可逾越的六大业务与合规红线

所有参与编码的 Agent **必须严格遵守以下六大红线**：

1. **绝对防插队铁律 (Anti-Corruption Rule)**：
   - 释放的空闲车位只能由 `scheduler.py` 按照 FIFO（一车优先于二车、申请时间戳先后）自动分发；
   - **物业端管理后台代码中坚决禁止出现任何“手动将车位直接指派给指定人员”的 API 接口或界面按钮**！
2. **政务最小化与隐私合规铁律 (Privacy Compliance Rule)**：
   - 身份证、房产证、结婚证照片在前端完成 1 秒拟真扫描识别后，仅提取证号与姓名；
   - **后端数据库只存储脱敏信息、证号与验真结果哈希，绝不在服务器磁盘上持久化存储高清扫描件图片**！
3. **外区房产强制拦截铁律 (Boundary Check Rule)**：
   - 调取出的名下房产清单中，外区房产（如浦东新区世纪大道）在前端强制置灰，带“非本小区”不可选标签；强行点击时必须弹出阻断性 Toast 警告：“仅限本小区产权房屋可申请车位”，后端提交接口同样执行二次强校验。
4. **夫妻车辆智能弹窗引导铁律 (Spouse Car Guidance Rule)**：
   - 申请页默认不显示结婚证上传；当系统比对发现行驶证所有人与房产所有人姓名不一致时，自动弹窗引导：“检测到车主姓名不符，是否为配偶车辆？”，用户确认后才动态展开结婚证上传区域。
5. **一房多车排位降权铁律 (Second Car Priority Drop Rule)**：
   - 若同一房屋已在排队或已有在租车位，第二辆车允许申请，但系统自动标注 `priority_level = 2 (家庭二车)`，其顺位严格排在所有“一车”申请之后。
6. **48小时严格倒计时与超时清退铁律 (48h Strict Timeout Rule)**：
   - 队首中签业主享有精确到秒的 48 小时确认期；倒计时结束后未办理，系统自动判定为放弃出列，车位自动顺延至下一位。

---

## 3. 数据库实体模型 (SQLite DDL & SQLAlchemy 2.0)

所有数据表均定义在 `backend/models.py` 中：

```python
from sqlalchemy import Column, Integer, String, Boolean, DateTime, Date, ForeignKey, Text
from sqlalchemy.orm import declarative_base, relationship
from datetime import datetime

Base = declarative_base()

class Community(Base):
    """小区基础表"""
    __tablename__ = "communities"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False) # 某某新村
    district = Column(String(50), nullable=False) # 徐汇区
    subdistrict = Column(String(50), nullable=False) # 湖南路街道
    total_spaces = Column(Integer, default=50) # 总车位数 50

class User(Base):
    """用户账号表 (业主主账号 / 家庭副账号 / 物业管理员)"""
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    phone = Column(String(20), unique=True, index=True, nullable=False)
    id_card = Column(String(18), unique=True, index=True, nullable=False)
    password_hash = Column(String(128), nullable=False)
    real_name = Column(String(50), nullable=False)
    role = Column(String(20), default="OWNER") # OWNER(主申请人), FAMILY(家庭成员副账号), PROPERTY_ADMIN(物业)
    community_id = Column(Integer, ForeignKey("communities.id"), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class ParkingSpace(Base):
    """车位资产台账表"""
    __tablename__ = "parking_spaces"
    id = Column(Integer, primary_key=True, index=True)
    community_id = Column(Integer, ForeignKey("communities.id"), nullable=False)
    space_no = Column(String(30), unique=True, nullable=False) # A-001 ~ A-050
    area_type = Column(String(20), default="GROUND") # GROUND(地面), UNDERGROUND(地下)
    status = Column(String(30), default="VACANT") 
    # VACANT(空闲), ALLOCATED(已分配待确认48h), OFFLINE_PENDING(线下办理中), RENTED(已租), MAINTENANCE(维修停用)
    current_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

class QueueApplication(Base):
    """车位排队申请单表"""
    __tablename__ = "queue_applications"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    community_id = Column(Integer, ForeignKey("communities.id"), nullable=False)
    property_no = Column(String(100), nullable=False) # 12号楼302室
    plate_no = Column(String(20), nullable=False) # 沪A·12345
    is_spouse_car = Column(Boolean, default=False)
    priority_level = Column(Integer, default=1) # 1: 家庭一车, 2: 家庭二车
    current_rank = Column(Integer, default=0) # 实时排位 (1, 2, 3...)
    status = Column(String(30), default="WAITING") 
    # WAITING(排队中), ALLOCATED(中签待确认48h), OFFLINE_PENDING(线下办理中), COMPLETED(租约生效), CANCELLED(主动撤销), TIMEOUT_CANCELLED(超时放弃)
    allocated_space_id = Column(Integer, ForeignKey("parking_spaces.id"), nullable=True)
    allocated_time = Column(DateTime, nullable=True) # 48h 倒计时起始点
    apply_time = Column(DateTime, default=datetime.utcnow, index=True) # FIFO 排序键

class RentalContract(Base):
    """车位租约履约表"""
    __tablename__ = "rental_contracts"
    id = Column(Integer, primary_key=True, index=True)
    contract_no = Column(String(64), unique=True, nullable=False) # AGY-CT-20260907-XXXX
    space_id = Column(Integer, ForeignKey("parking_spaces.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False) # 默认1年租期
    status = Column(String(30), default="ACTIVE") 
    # ACTIVE(生效中), EXPIRING(临期<=30天), OVERDUE(逾期宽限期7天), TERMINATED(已退租)
    created_at = Column(DateTime, default=datetime.utcnow)

class ChainEvidence(Base):
    """虚拟区块链存证流水表"""
    __tablename__ = "chain_evidence"
    id = Column(Integer, primary_key=True, index=True)
    evidence_id = Column(String(64), unique=True, index=True, nullable=False) # AGY-EVID-xxx
    community_id = Column(Integer, nullable=False)
    user_id = Column(Integer, nullable=True)
    event_type = Column(String(32), nullable=False) # APPLY, CANCEL, ALLOCATE, SIGN, RENEW, TERMINATE, INIT
    block_height = Column(Integer, nullable=False) # 虚拟自增区块高度
    tx_hash = Column(String(66), nullable=False) # 0x + 64位哈希
    prev_hash = Column(String(66), nullable=False) # 形成链式结构
    event_summary = Column(String(255), nullable=False) # 中文摘要
    raw_payload_json = Column(Text, nullable=False) # 业务快照数据
    timestamp = Column(String(32), nullable=False) # 毫秒级时间戳
    created_at = Column(DateTime, default=datetime.utcnow)

class Notification(Base):
    """消息通知表"""
    __tablename__ = "notifications"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    title = Column(String(100), nullable=False)
    content = Column(Text, nullable=False)
    type = Column(String(30), nullable=False) # RANK_CHANGE, ALLOCATION, EXPIRATION, OVERDUE, SYSTEM
    is_read = Column(Boolean, default=False)
    action_type = Column(String(30), nullable=True) # GO_CONFIRM, GO_QUEUE
    created_at = Column(DateTime, default=datetime.utcnow)
```

---

## 4. 全量 RESTful API 契约与 JSON Schema 规范

所有端点均定义在 `backend/routers/`，统一遵循标准响应结构：`{"code": 200, "message": "success", "data": ...}`。

### 4.1 认证模块 (`/api/auth`)
- `POST /api/auth/login`
  - **Request**: `{"identifier": "13800000001", "password": "...", "login_type": "PASSWORD"}` (支持手机号或身份证号)
  - **Response**: `{"code": 200, "data": {"token": "...", "user": {"id": 1, "real_name": "张欣欣", "role": "OWNER", "community_name": "某某新村"}}}`
- `POST /api/auth/register`
  - **Request**: `{"district": "徐汇区", "subdistrict": "湖南路街道", "community_name": "某某新村", "real_name": "...", "id_card": "...", "phone": "...", "password": "..."}`

### 4.2 业主排位模块 (`/api/parking`)
- `GET /api/user/home` (主页动态核心接口)
  - **Response**:
    ```json
    {
      "code": 200,
      "data": {
        "view_state": "WAITING", // RENTED | NO_APPLY | WAITING | ALLOCATED | OFFLINE_PENDING
        "user_info": { "real_name": "张欣欣", "community_name": "某某新村" },
        "unread_notifications": 2,
        "my_space": { "space_no": "A-008", "plate_no": "沪A·12345", "start_date": "2025-10-01", "end_date": "2026-10-01", "days_left": 24 },
        "my_queue": {
          "current_rank": 3,
          "total_queue": 45,
          "is_first_candidate": false,
          "apply_time": "2026-09-01 10:00:00",
          "ai_waiting_time": "约 2~3 个月",
          "ai_estimation_basis": "基于小区近一年年化退租周转率 8% 测算",
          "allocated_space_no": "A-012",
          "countdown_seconds": 172340
        },
        "community_overview": { "total_spaces": 50, "rented_spaces": 46, "vacant_spaces": 2, "maintenance_spaces": 1 },
        "public_spaces": [
          { "space_no": "A-001", "plate_no": "沪A·***12", "owner_name": "王*", "room_no": "10号楼 20*室", "end_date": "2027-01-01" }
        ]
      }
    }
    ```
- `POST /api/parking/apply` (三证合一提交)
  - **Request**: `{"property_no": "12号楼302室", "plate_no": "沪A·67890", "is_spouse_car": true, "marriage_cert_no": "J310104-2022-0089"}`
  - **校验拦截**: 若 `property_no` 属于外区房，强制返回 `{"code": 400, "message": "仅限本小区产权房屋可申请车位"}`。
- `POST /api/parking/cancel-apply` (主动撤销排队)
  - 确认后移出队列，自动触发 `scheduler.trigger_recalculate_queue()`，返回成功。
- `POST /api/parking/accept-allocation` (队首确认前往线下办理)
  - 将申请状态由 `ALLOCATED` 切换为 `OFFLINE_PENDING`。
- `GET /api/parking/queue` (顺位全队列脱敏公示)
  - **Response**: 返回全量脱敏队列列表：`[{"rank": 1, "masked_name": "李*", "masked_phone": "138****0002", "apply_time": "2026-08-15 10:20:00", "priority_type": "家庭一车"}]`。
- `GET /api/parking/my-timeline` (本人排位轨迹时间线)
  - 返回本人顺位前移的历程事件列表。

### 4.3 物业管理中台模块 (`/api/property`)
- `POST /api/property/init-demo-spaces` (一键注入 50 个车位演示数据)
- `POST /api/property/set-maintenance` (维修停用/恢复可用)
  - **Request**: `{"space_id": 50, "action": "SET_MAINTENANCE"}` (或 `RESTORE`)
  - 恢复可用时，自动触发排位检测分发。
- `POST /api/property/confirm-delivery` (线下签约交接完成)
  - **Request**: `{"space_id": 12, "user_id": 2}`
  - 正式将车位转为 `RENTED`，生成 1 年期正式租约与区块链存证。
- `POST /api/property/force-terminate` (物业代清退)
  - **Request**: `{"space_id": 8, "reason": "房屋买卖过户且业主失联", "attachment_url": "mock_proof.jpg"}`
  - 释放车位，自动触发调度引擎，后序顺位秒级前移。

### 4.4 存证与开发模块 (`/api/evidence` & `/api/dev`)
- `GET /api/evidence/list` (个人存证中心)
  - 返回当前用户的存证流水明细与防伪摘要。
- `POST /api/dev/reset-seed` (一键重置演示数据)
  - 瞬间清空并重新灌入张欣欣、李雷、韩梅梅、王建国等初始状态，防演示翻车！

---

## 5. 调度引擎核心逻辑与原子重排算法 (`scheduler.py`)

调度引擎是系统的核心枢纽，严禁任何人工插队：

```python
from datetime import datetime
from sqlalchemy.orm import Session
from models import ParkingSpace, QueueApplication, Notification
import evidence

def trigger_recalculate_queue(db: Session, community_id: int):
    """
    排位重排与自动分配核心引擎 (原子事务):
    1. 获取小区内所有 VACANT 空闲车位;
    2. 按 priority_level (一车优先) 升序、apply_time 升序获取 WAITING 队列;
    3. 自动匹配分发: 车位与申请均变更为 ALLOCATED, 记录 allocated_time (48h基准);
    4. 剩余 WAITING 申请人重新赋予连续 current_rank (1, 2, 3...);
    5. 若顺位发生前移, 自动写入顺位变动通知与时间线记录。
    """
    vacant_spaces = db.query(ParkingSpace).filter(
        ParkingSpace.community_id == community_id,
        ParkingSpace.status == "VACANT"
    ).order_by(ParkingSpace.space_no.asc()).all()

    waiting_apps = db.query(QueueApplication).filter(
        QueueApplication.community_id == community_id,
        QueueApplication.status == "WAITING"
    ).order_by(
        QueueApplication.priority_level.asc(),
        QueueApplication.apply_time.asc()
    ).all()

    # 1. 自动分配空闲车位
    while vacant_spaces and waiting_apps:
        space = vacant_spaces.pop(0)
        app = waiting_apps.pop(0)

        space.status = "ALLOCATED"
        space.current_user_id = app.user_id

        app.status = "ALLOCATED"
        app.allocated_space_id = space.id
        app.allocated_time = datetime.utcnow()
        app.current_rank = 0 # 获得分配后不再占排位次序

        # 存证
        evidence.create_record(
            db=db,
            community_id=community_id,
            user_id=app.user_id,
            event_type="ALLOCATE",
            summary=f"系统自动分配车位【{space.space_no}】(启动48小时倒计时)",
            payload={"space_no": space.space_no, "allocated_time": str(app.allocated_time)}
        )

        # 触发 48h 中签通知
        db.add(Notification(
            user_id=app.user_id,
            title="车位锁定中签提醒",
            content=f"恭喜！系统已为您锁定车位【{space.space_no}】，请在 48 小时内确认前往物业办公室办理线下签约缴费！",
            type="ALLOCATION",
            action_type="GO_CONFIRM"
        ))

    # 2. 连续重排剩余队列
    for rank, app in enumerate(waiting_apps, start=1):
        if app.current_rank != rank:
            old_rank = app.current_rank
            app.current_rank = rank
            # 若顺位前移，生成通知
            if old_rank > rank or old_rank == 0:
                db.add(Notification(
                    user_id=app.user_id,
                    title="排队顺位前移提醒",
                    content=f"由于前序车位释放，您的实时排位已升至第【{rank}】位！",
                    type="RANK_CHANGE",
                    action_type="GO_QUEUE"
                ))
    db.commit()
```

---

## 6. 虚拟区块链存证服务与防伪凭据算法 (`evidence.py`)

底层使用 `chain_evidence` 表，模拟产生具备数学真实性的哈希链表与防伪凭据：

```python
import hashlib, time, uuid, json
from sqlalchemy.orm import Session
from models import ChainEvidence

def create_record(db: Session, community_id: int, user_id: int, event_type: str, summary: str, payload: dict):
    last_evidence = db.query(ChainEvidence).order_by(ChainEvidence.id.desc()).first()
    prev_hash = last_evidence.tx_hash if last_evidence else "0x0000000000000000000000000000000000000000000000000000000000000000"
    block_height = (last_evidence.block_height + 1) if last_evidence else 1829001
    
    timestamp = str(int(time.time() * 1000))
    raw_str = f"{event_type}_{community_id}_{user_id}_{timestamp}_{prev_hash}_{json.dumps(payload, sort_keys=True)}"
    tx_hash = "0x" + hashlib.sha256(raw_str.encode('utf-8')).hexdigest()
    evidence_id = f"AGY-{event_type}-{timestamp[-6:]}-{uuid.uuid4().hex[:4].upper()}"
    
    record = ChainEvidence(
        evidence_id=evidence_id,
        community_id=community_id,
        user_id=user_id,
        event_type=event_type,
        block_height=block_height,
        tx_hash=tx_hash,
        prev_hash=prev_hash,
        event_summary=summary,
        raw_payload_json=json.dumps(payload, ensure_ascii=False),
        timestamp=timestamp
    )
    db.add(record)
    db.commit()
    return record
```

---

## 7. 前端 Vue 3 + Tailwind CSS 界面与动态视图设计

### 7.1 移动端与 PC 响应式适配架构
- **移动端视口 (Mobile H5)**：页面采用自适应容器 `<div class="max-w-md mx-auto min-h-screen bg-slate-50">`，在手机浏览器或移动调试模式下呈现原生 App 级的卡片交互；
- **PC 宽屏管理端 (Property Middle Office)**：路由 `/property/dashboard` 自动适应宽屏，左侧沉浸式深蓝导航树，右侧数据看板 + 车位矩阵网格 + 待办抽屉。

### 7.2 核心组件规格清单
1. **`SeedAccountBar.vue` (顶部快捷账号条)**：
   - 固定在登录页上方，4 个醒目按钮一键填入：`[填入张欣欣(未申请)]`、`[填入李雷(中签48h)]`、`[填入韩梅梅(在租临期)]`、`[填入王建国(物业管理)]`，极度贴心实用！
2. **`CountdownCard.vue` (48h 动态倒计时中签卡片)**：
   - 渐变金色背景，展示“已为您锁定车位【A-012】”；
   - 实时跳动时分秒组件（如 `47 : 58 : 22`）；
   - 提供【确认前往办理】（切入线下办理中）与【放弃本次机会】操作。
3. **`EvidenceModal.vue` (区块链存证证书防伪弹窗)**：
   - 弹出古典科技风羊皮纸/科技蓝存证卡；
   - 完整 66 位交易哈希（点击带“已复制”小气泡）；
   - 绿色公章水印“【上海市基层治理数据服务中心·区块链存证验真通过】”。
4. **`OcrUploadBox.vue` (1秒拟真 OCR 扫描组件)**：
   - 点击选择图片后，展示绿色雷达扫描光带在图片上平移 1 秒；
   - 随后自动带出姓名与证件号，展示政务互通感。

---

## 8. 开箱即用演示预置数据矩阵与一键重置机制 (`seed.py`)

运行 `python seed.py` 必须自动建立以下数据矩阵：

### 8.1 四大核心演示账号
| 角色 | 手机号 | 密码 | 姓名 | 身份证号 | 初始业务状态 | 演示任务 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **业主 A** | `13800000001` | `123456` | 张欣欣 | `310104199201011111` | **未申请新客**<br>(名下本小区房+外区房+夫妻车) | 第一幕：三证核验、外区房拦截、夫妻车引导 |
| **业主 B** | `13800000002` | `123456` | 李雷 | `310104199002022222` | **队首中签者**<br>(分配 A-012，倒计时 47:50:00) | 第四幕：中签卡片、48h 倒计时、确认签约存证 |
| **业主 C** | `13800000003` | `123456` | 韩梅梅 | `310104199103033333` | **已有车位业主**<br>(承租 A-008，剩余 25 天) | 展示已有车位进度条、临期催缴与主动退租 |
| **物业** | `13900000001` | `123456` | 王建国 | `310104198005055555` | **物业公司负责人**<br>(管理某某新村 50 车位) | 第三幕：PC 宽屏大盘、强制代清退上链 |

### 8.2 50 个车位初始状态配比
- `A-001 ~ A-045`：已租 (`RENTED`)，分配给虚拟业主；
- `A-008`：已租 (`RENTED`)，承租人为韩梅梅；
- `A-012`：中签待确认 (`ALLOCATED`)，承租人为李雷；
- `A-048, A-049`：空闲 (`VACANT`)；
- `A-050`：维修停用 (`MAINTENANCE`)。

---

## 9. 周三向张总汇报：经典四幕演示链路自动化保障

前端在 `HomeView.vue` 中内置 **3 秒轻量轮询**，确保汇报时双端动作秒级联动：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       周三张总评审黄金演示四幕流程                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ 【第一幕：严审准入与免材料核验】(业主端H5 - 张欣欣)                         │
│  1. 登录张欣欣账号 -> 点击【申请车位】 -> 上传身份证照片 (1秒OCR拟真扫描)   │
│  2. 调取电子证照 -> 故意点击“浦东外区房” -> 系统立即弹出置灰与拦截警告 Toast │
│  3. 勾选本小区房产 -> 选配偶名下车辆 -> 系统弹窗引导：“是否为夫妻车？请上传” │
│  4. 上传结婚证 -> 提交成功 -> 生成排位 (第 3 位) -> 展示 AI 预估等待 2~3 个月 │
├─────────────────────────────────────────────────────────────────────────────┤
│ 【第二幕：阳光透明与全队列监督】(业主端H5 - 张欣欣)                         │
│  1. 点击顺位卡片进入【顺位公示页】 -> 展现全小区脱敏大盘 (张**，138****0001)│
│  2. 查看个人排位时间线：展示刚刚提交申请、存证上链的时间戳记录              │
├─────────────────────────────────────────────────────────────────────────────┤
│ 【第三幕：公平流转与秒级联动】(物业PC中台 + 业主端H5 同屏联动 - 全场最高潮) │
│  1. PC 浏览器打开物业后台登录王建国 -> 展示 50 车位大屏看板                 │
│  2. 找到违规/到期车位 -> 点击【强制清退/代办退租】 -> 选择买卖过户原因并上传凭证│
│  3. 点击提交 -> 车位释放成功！                                              │
│  4. 切回张欣欣手机端 -> 3秒内排位数字由“第3位”自动跳变为“第2位”，弹出通知！│
├─────────────────────────────────────────────────────────────────────────────┤
│ 【第四幕：中签履约与区块链存证】(双端闭环 - 李雷 & 王建国)                  │
│  1. 登录李雷账号 -> 首页展示金色高亮卡片与 48h 确认动态倒计时 (47:58:22)   │
│  2. 点击【确认前往办理】 -> 状态进入“线下办理中”                           │
│  3. 物业端后台点击【确认交付完成】 -> 车位正式转为“已租” (365天生效)        │
│  4. 切换李雷端【区块链存证中心】 -> 展示防伪电子证书，一键复制 SHA-256 哈希 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. 工程一键启动、本地离线化运行与交付自测标准

### 10.1 一键启动脚本规范 (`run.sh`)
在项目根目录下提供 `run.sh`，确保用户或评审人员只需一条命令即可跑通：
```bash
#!/usr/bin/env bash
set -e
echo "=== 正在启动老旧小区停车位管理系统全栈服务 ==="

# 1. 后端准备
cd backend
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi
source venv/bin/activate
pip install -r requirements.txt -q
python seed.py # 初始化数据库与Seed数据
uvicorn main:app --host 0.0.0.0 --port 8000 &
BACKEND_PID=$!
cd ..

# 2. 前端准备
cd frontend
if [ ! -d "node_modules" ]; then
    npm install
fi
npm run dev &
FRONTEND_PID=$!
cd ..

echo ">>> 后端接口文档: http://localhost:8000/docs"
echo ">>> 前端系统地址: http://localhost:5173"
echo ">>> 系统已就绪，按 Ctrl+C 停止运行。"

trap "kill $BACKEND_PID $FRONTEND_PID" EXIT
wait
```

### 10.2 明天(周二)下班交付自测验收标准
所有负责编写代码的 Agent 在宣称完成任务前，**必须执行以下 6 项冒烟自测**：
1. [ ] 运行 `bash run.sh`，5 秒内双端顺利拉起，无端口冲突或缺少依赖报错；
2. [ ] 点击快捷填充张欣欣登录，点击外区房产，系统必须准确拦截并弹窗警告；
3. [ ] 勾选配偶车辆，必须正确弹出“夫妻车结婚证上传引导”，上传后成功入队排在第 3 位；
4. [ ] 打开物业管理后台，点击清退车位后，张欣欣端必须在 3 秒内自动前移至第 2 位；
5. [ ] 登录李雷账号，48 小时倒计时时分秒必须正常跳动，点击【确认前往办理】后物业端可完成交付；
6. [ ] 区块链存证弹窗必须能正确展示 66 位哈希、区块高度与防伪盖章。
