# 慧停管家 · 老旧小区停车位管理系统 部署说明文档

---

## 一、 系统架构与部署方案概述

本系统为前后端分离架构，针对老旧小区停车位“申报、排队、轮候、公示、审核、存证、审计”全流程进行数字化管理：

- **前端架构**：Vue 3 + Vite + Tailwind CSS + Pinia，由 **Nginx (Alpine)** 容器托管静态资源并负责反向代理。
- **后端架构**：Java 17 (OpenJDK 17) + Spring Boot 3.4.4 + Spring Data JPA，容器化运行 Spring Boot 独立应用。
- **数据库架构**：SQLite 3（轻量化嵌入式文件数据库，单文件零配置免维护，通过 Docker 卷持久化至宿主机）。
- **部署方式**：标准 **Docker Compose** 双容器编排。

---

## 二、 部署环境与配置要求

### 1. 硬件配置要求
| 硬件项 | 最低要求 | 推荐配置 | 说明 |
| :--- | :--- | :--- | :--- |
| **CPU** | 2 核 | 4 核及以上 | 容器运行及高并发处理 |
| **内存** | 4 GB | 8 GB 及以上 | Java 17 JVM 运行占用约 500M~1G，Nginx 极低 |
| **磁盘空间** | 可用 20 GB | 50 GB 以上 | 用于 Docker 镜像层缓存与数据持久化 |

### 2. 操作系统与软件依赖
- **操作系统**：Linux 64 位（推荐 Ubuntu 20.04/22.04 LTS 或 CentOS 7.9+ / Debian 11+）。
- **Docker 引擎**：Docker Engine 20.10.x 及以上（推荐 Docker 24.0+）。
- **Docker Compose**：Docker Compose v2.x（如 `docker compose` 命令）或 1.29+。
- **基础镜像依赖**：
  - Java 运行环境镜像：`docker.1panel.live/library/openjdk:17-jdk-slim`（或 `openjdk:17-jdk-slim`）
  - 前端网关镜像：`nginx:alpine`
- **构建环境（如需在服务器本地编译）**：
  - JDK 17+、Apache Maven 3.8+、Node.js 18+、npm 9+（如在本地编译后上传产物，则服务器无需安装此套工具链）。

### 3. 网络与防火墙端口要求
| 端口号 | 协议 | 映射方向 | 用途与说明 |
| :--- | :--- | :--- | :--- |
| **39080** | TCP | 宿主机:39080 -> 容器:80 | **系统对外主服务端口**（Web 网页端及 API 访问） |
| **39000** | TCP | 宿主机:39000 -> 容器:8080 | 后端 Spring Boot 调试暴露端口（内网） |
| **1024 / 22** | TCP | 宿主机 SSH 端口 | 服务器远程运维管理 |

> **安全组提示**：若有云防火墙或企业内网安全网关，请放行 **39080** 端口的 TCP 入站流量。

---

## 三、 部署位置与目录规范

### 1. 服务器部署绝对路径
```bash
/DATA/QC_yangjichang/liugang_xiaoliu
```

### 2. 目录与文件布局规范
```text
/DATA/QC_yangjichang/liugang_xiaoliu/
├── docker-compose.yml              # Docker 编排配置（定义前后端容器编排、端口与网络）
├── .env                            # 环境变量文件（定义 PORT=39080、BACKEND_PORT=39000）
├── DEPLOYMENT.md                   # 部署说明文档
│
├── backend/                        # Java 后端工程目录（纯 Java Maven 结构）
│   ├── pom.xml                     # Maven 依赖与项目配置文件
│   ├── Dockerfile                  # 后端 Dockerfile（基于 OpenJDK 17 Slim 镜像）
│   ├── src/                        # Java 源代码与配置资源目录
│   │   └── main/
│   │       ├── java/               # 核心业务源码（Controller, Service, Entity 等）
│   │       └── resources/          # application.yml 及字典配置
│   └── target/
│       └── parking-java-1.0.0.jar  # 生产构建的可执行 Spring Boot Jar 包
│
├── frontend/                       # Vue 3 前端工程目录
│   ├── Dockerfile                  # 前端 Dockerfile（基于 Nginx Alpine 镜像）
│   ├── nginx.conf                  # Nginx 反向代理配置（代理 SPA 路由与 /api/ 请求）
│   └── dist/                       # 前端 npm run build 编译输出的静态资产目录
│       ├── index.html
│       └── assets/
│
└── data/                           # 数据持久化挂载目录（绝对不可随意删除！）
    ├── parking.db                  # 当前生产 SQLite 数据库（挂载进入容器 /app/data）
    └── parking.db.pybak            # 备份数据库文件
```

---

## 四、 详细部署操作步骤

### 步骤一：本地代码编译与产物生成（推荐本地完成）

在开发机或者 CI/CD 机器上完成前后端生产打包：

1. **编译后端 Java Jar 包**：
   ```powershell
   cd backend
   mvn clean package -DskipTests
   # 在 backend/target/ 下将生成 parking-java-1.0.0.jar
   ```

2. **编译前端静态文件**：
   ```powershell
   cd frontend
   npm install
   npm run build
   # 在 frontend/ 下将生成包含静态资源的 dist/ 目录
   ```

---

### 步骤二：上传文件至服务器指定部署目录

通过 SCP 或 SFTP 工具将以下文件与目录传输至服务器 `/DATA/QC_yangjichang/liugang_xiaoliu`：

- `docker-compose.yml`
- `.env`
- `data/parking.db`
- `backend/Dockerfile`、`backend/target/parking-java-1.0.0.jar`、`backend/pom.xml`、`backend/src/`
- `frontend/Dockerfile`、`frontend/nginx.conf`、`frontend/dist/`

---

### 步骤三：环境配置检查与修正

登录服务器并进入部署目录：
```bash
ssh -p 1024 qc_yangjichang@192.168.0.147
cd /DATA/QC_yangjichang/liugang_xiaoliu
```

检查 `.env` 配置，确认端口无误：
```ini
PORT=39080
BACKEND_PORT=39000
```

---

### 步骤四：设置持久化数据目录权限（至关重要）

SQLite 为嵌入式单文件数据库，运行在 Docker 容器内部的进程必须具备对其父目录 `data/` 及 `parking.db` 文件的写权限，执行：

```bash
chmod -R 777 /DATA/QC_yangjichang/liugang_xiaoliu/data
```

---

### 步骤五：拉起 Docker 容器组并构建

在 `/DATA/QC_yangjichang/liugang_xiaoliu` 目录下执行容器拉起命令：

```bash
# 构建并启动双容器（后台常驻）
docker compose up -d --build
```

---

### 步骤六：部署验证

1. **检查容器运行状态**：
   ```bash
   docker compose ps
   ```
   *预期输出*：`parking-backend` 和 `parking-frontend` 两个容器状态均为 `Up`。

2. **检查服务健康接口**：
   ```bash
   curl -s http://127.0.0.1:39080/api/health
   ```
   *预期输出*：`{"code":200,"message":"系统运行正常","data":{"status":"UP"}}`

3. **浏览器访问系统**：
   在浏览器直接打开：`http://192.168.0.147:39080/`

---

## 五、 部署与运维核心注意事项

### 1. 数据持久化与定期备份机制（防丢失）
- **挂载规则**：宿主机目录 `./data` 挂载到后端容器内部的 `/app/data`。无论容器如何重启、销毁重建，**所有用户、申请、车辆、日志数据均保存在宿主机的 `data/parking.db` 中**。
- **备份建议**：在进行任何系统大版本更新或迁移前，务必先备份该文件：
  ```bash
  cp data/parking.db data/parking.db.$(date +%Y%m%d_%H%M%S).bak
  ```

### 2. SQLite 读写锁与并发注意事项
- **单写多读特性**：SQLite 支持高频并发读取，但同一毫秒仅允许一个事务进行写操作。
- **HikariCP 连接池配置**：后端 `application.yml` 已配置 HikariCP `maximum-pool-size: 1` 并开启 `PRAGMA foreign_keys=ON`，避免发生文件死锁（`database is locked`）。**请勿随意调大连接池最大连接数**。

### 3. Nginx 转发与路由配置要点
- **SPA 路由回退**：前端使用 Vue Router HTML5 History 模式，`nginx.conf` 必须包含：
  ```nginx
  location / {
      try_files $uri $uri/ /index.html;
  }
  ```
  保证刷新页面（如 `/admin/login`、`/property/dashboard`）不会出现 Nginx 404。
- **反向代理后端**：所有 `/api/` 请求经由 Nginx 分发至后端：
  ```nginx
  location /api/ {
      proxy_pass http://backend:8080/api/;
  }
  ```

### 4. 端口冲突与自定义修改
- 如果宿主机的 `39080` 或 `39000` 端口被其他服务占用，**切勿直接修改 Dockerfile**，只需编辑 `.env` 文件：
  ```ini
  PORT=新端口号
  BACKEND_PORT=新后端端口号
  ```
  然后执行 `docker compose up -d` 即可自动重新绑定端口。

### 5. 权限安全与默认账号保管
系统初始化播种的默认演示账号如下，生产环境请务必及时在后台修改或在数据库中变更：
- **系统管理员**：`admin` / `admin123`（后台登录入口：`#/admin/login`）
- **物业管理员**：`13900000001` / `123456`（物业端入口：`#/property/login`）
- **业主演示账号**：`13800000001`（张欣欣） / `123456`

---

## 六、 常用运维命令速查表

| 操作需求 | 执行命令 |
| :--- | :--- |
| **查看服务容器状态** | `docker compose ps` |
| **实时查看后端 Java 日志** | `docker logs -f parking-backend --tail 100` |
| **实时查看前端 Nginx 访问日志** | `docker logs -f parking-frontend --tail 100` |
| **重启整个服务集群** | `docker compose restart` |
| **仅重启后端 Java 服务** | `docker compose restart backend` |
| **更新前端静态资源后重载** | `docker cp frontend/dist/. parking-frontend:/usr/share/nginx/html/ && docker exec parking-frontend nginx -s reload` |
| **停止并卸载容器组** | `docker compose down` |
| **容器内执行健康检查** | `docker exec parking-backend curl -s http://127.0.0.1:8080/api/health` |
