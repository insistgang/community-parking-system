# 老旧小区停车位管理系统

面向老旧小区车位供需失衡治理的**公平排位、三证合一准入、虚拟区块链存证**全栈演示系统。基于产品需求规格说明书 PRD-V3.0 落地，纯离线自包含、零外部 CDN 依赖，专为 **2026-09-09 向张总汇报评审** 打造。

---

## ✨ 核心特性

### 业主移动端 (H5)
- **双通道登录**：账号密码 + 短信验证码（演示码 `888888`），登录页内置四大演示账号一键填充
- **三证合一申请**：身份证/结婚证 OCR 拟真扫描（1 秒雷达光带动效），仅提取证号与姓名，服务端不持久化任何证件图片
- **外区房强制拦截**：名下外区房产（如浦东新区世纪大道）置灰不可选，前端阻断 Toast + 后端二次强校验
- **夫妻车智能引导**：行驶证所有人与房产所有人姓名不一致时，自动弹窗引导上传结婚证
- **动态 4 重视图**：`NO_APPLY`（未申请）/ `WAITING`（排队中）/ `ALLOCATED`（中签 48h 金色倒计时）/ `RENTED`（在租含临期催缴进度条）
- **阳光公示**：全队列脱敏公示（张\*\*、138\*\*\*\*0001）、个人排位轨迹时间线、AI 预估等待时长
- **区块链存证中心**：66 位 SHA-256 交易哈希一键复制、区块高度、防伪验真证书弹窗

### 物业管理中台 (PC 宽屏)
- 50 车位资产大屏看板与台账矩阵（地面/地下、已租/空闲/维修状态一览）
- 维修停用 / 恢复可用（恢复后自动参与顺位分配）
- 线下签约交付确认（生成 1 年期正式租约并上链）
- 强制代清退（上传凭证、填写原因），释放车位秒级顺延
- **无任何手动指派车位入口**——车位只能由调度引擎按 FIFO 自动分发

### 调度引擎与存证
- **FIFO 原子重排**：一车优先于二车（`priority_level`）、同优先级按申请时间戳排序
- **48h 严格倒计时**：中签后精确到秒确认期，超时自动判定放弃出列并顺延（后台每分钟定时清扫 + 主页访问惰性触发，双层保障）
- **顺位秒级联动**：物业端清退 → 业主端 3 秒轮询内排位自动前移并推送通知
- **虚拟区块链**：哈希链式结构（`tx_hash` / `prev_hash`）、自增区块高度、业务快照全文上链、验真接口

---

## 🛡️ 六大业务红线实现状态

| 红线 | 实现 |
|---|---|
| 1. 绝对防插队 | ✅ 物业端无任何手动指派接口，车位仅由 `scheduler.py` FIFO 分发 |
| 2. 政务最小化隐私合规 | ✅ 证件仅前端拟真识别，后端只存证号/姓名/验真哈希 |
| 3. 外区房产强制拦截 | ✅ 前端置灰阻断 + 后端 `POST /api/parking/apply` 二次强校验 |
| 4. 夫妻车辆弹窗引导 | ✅ 姓名不一致自动弹窗，确认后展开结婚证上传区 |
| 5. 一房多车排位降权 | ✅ 同房第二车自动 `priority_level=2`，严格排在一车之后 |
| 6. 48h 超时清退 | ✅ 超时自动收回顺延，通知 + 存证完整闭环 |

---

## 🧰 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Python 3.10+ · FastAPI · SQLAlchemy 2.0 · SQLite 3（单文件 `parking.db`，零配置） |
| 前端 | Vue 3 (Composition API + `<script setup>`) · Vite · Tailwind CSS 3.x · Pinia · Vue Router 4 |
| 其他 | 内嵌 SVG 图标（无外链）、CSS 拟真扫描/倒计时动效、Swagger 自动文档 |

---

## 📁 项目结构

```text
├── run.sh                        # 全栈一键启动脚本（演示首选）
├── push.sh                       # 一键提交并推送 GitHub
├── agents.md                     # 工程开发宪法（业务规则与验收标准）
├── DEMO_GUIDE.md                 # 演示操作手册
├── 老旧小区停车位管理系统_开发PRD文档_v3.0.md   # 产品需求终审基线
├── backend/
│   ├── main.py                   # FastAPI 入口（CORS / 全局异常 / 48h 后台清扫任务）
│   ├── models.py                 # 7 张核心数据表 ORM
│   ├── scheduler.py              # FIFO 原子重排引擎 + 48h 超时清扫 + AI 等待预估
│   ├── evidence.py               # 虚拟区块链存证服务（SHA-256 哈希链）
│   ├── seed.py                   # 演示数据矩阵初始化与重置
│   ├── districts.py              # 上海 16 区划街道级联数据
│   └── routers/                  # auth / parking / property / evidence / dev
└── frontend/
    └── src/
        ├── api/                  # Axios 封装（X-User-Id 鉴权头）
        ├── store/                # Pinia 用户状态
        ├── router/               # 双端路由
        ├── components/           # 倒计时卡 / 存证弹窗 / OCR 扫描 / 账号填充条等
        └── views/                # 业主 H5 各页 + property/ 物业中台
```

---

## 🚀 快速开始

**前置要求**：Python 3.10+、Node.js 18+（无需外网，依赖装好后纯离线运行）

```bash
bash run.sh
```

脚本自动完成后端虚拟环境创建、依赖安装、演示数据初始化与双端拉起：

| 入口 | 地址 |
|---|---|
| 📱 业主移动端 H5 | http://localhost:5173/ |
| 🏢 物业管理中台 PC | http://localhost:5173/property/dashboard |
| 📚 Swagger API 文档 | http://localhost:8000/docs |

按 `Ctrl + C` 同时停止前后端服务。

---

## 👤 演示账号（密码均为 `123456`）

| 角色 | 手机号 | 姓名 | 初始状态 | 演示任务 |
|---|---|---|---|---|
| 业主 A | `13800000001` | 张欣欣 | 未申请新客（本小区房 + 外区房 + 夫妻车） | 第一幕：三证核验 / 外区房拦截 / 夫妻车引导 |
| 业主 B | `13800000002` | 李雷 | 队首中签（A-012，48h 倒计时约 47:50:00） | 第四幕：中签倒计时 / 确认签约 / 存证验真 |
| 业主 C | `13800000003` | 韩梅梅 | 在租临期（A-008，剩余 25 天） | 临期催缴 / 已有车位视图 |
| 物业 | `13900000001` | 王建国 | 物业管理员（50 车位） | 第三幕：PC 大屏 / 强制清退联动 |

> 队列中预置赵强（第 1 位）、钱小敏（第 2 位）两名虚拟业主，保证张欣欣申请后恰排第 3 位。

## 🎬 汇报演示四幕链路

1. **严审准入**（张欣欣）：一键填充登录 → 申请车位 → OCR 扫描身份证 → 点击浦东外区房被拦截 → 勾选配偶车辆弹窗引导 → 上传结婚证 → 入队第 3 位
2. **阳光透明**（张欣欣）：顺位公示页查看全小区脱敏队列与个人存证时间线
3. **公平流转**（王建国 + 张欣欣 同屏联动）：物业后台对 A-015（孙立新·逾期失联）执行强制清退 → 业主端 3 秒内排位 3→2 自动跳变并弹出通知
4. **中签履约**（李雷 + 王建国）：金色倒计时卡确认办理 → 物业端确认交付 → 租约生效 365 天 → 存证中心复制 SHA-256 防伪哈希

**防翻车保障**：任何演示操作后，访问 `POST /api/dev/reset-seed`（或重新运行 `seed.py`）即可一键恢复初始数据矩阵。

---

## 🔌 主要 API 一览

统一响应结构：`{"code": 200, "message": "success", "data": ...}`；鉴权方式：请求头 `X-User-Id`。

| 模块 | 端点 |
|---|---|
| 认证 | `POST /api/auth/login` · `POST /api/auth/send-sms` · `POST /api/register` · `GET /api/common/districts` |
| 业主 | `GET /api/user/home` · `POST /api/parking/apply` · `POST /api/parking/cancel-apply` · `POST /api/parking/accept-allocation` · `POST /api/parking/give-up-allocation` · `GET /api/parking/queue` · `GET /api/parking/my-timeline` · `GET /api/notifications` |
| 物业 | `GET /api/property/stats` · `GET /api/property/spaces` · `POST /api/property/set-maintenance` · `POST /api/property/confirm-delivery` · `POST /api/property/force-terminate` |
| 存证 | `GET /api/evidence/list` · `GET /api/evidence/verify/{evidence_id}` |
| 开发 | `POST /api/dev/reset-seed`（一键重置演示数据） |

完整契约见 Swagger 文档：http://localhost:8000/docs

---

## ⚠️ 说明

- 本系统为**汇报评审演示系统**：鉴权采用简化 `X-User-Id` 方案、区块链为虚拟实现，不可直接用于生产环境
- 所有时间戳统一 UTC 存储，48h 倒计时由服务端计算剩余秒数、前端本地跳动
- 详细业务规则与验收标准见 [agents.md](agents.md)，演示分镜脚本见 [DEMO_GUIDE.md](DEMO_GUIDE.md)
