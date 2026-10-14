# 慧停管家生产端部署

本目录用于新建 `/DATA/QC_yangjichang/liugang_produce`。现有测试目录 `liugang_xiaoliu`、端口 `39080/39000` 和测试数据库不参与此部署。

## 结构与端口

- 前端：Vue 3 构建产物由 Nginx 提供服务。内网验收临时绑定 `${PROD_BIND_IP}:39180`；公网启用有效证书后绑定 `39443`。
- 后端：Java 17 / Spring Boot，容器内 `8080`，没有宿主机端口映射。
- 数据库：MySQL 8.4，容器内 `3306`，没有宿主机端口映射。数据仅写入 `./mysql-data`。
- 前后端和数据库使用独立的 `parking-produce` Compose 项目及网络。
- 生产端从空库启动，不复制测试数据库，也不创建演示账号和演示车位。

## 本地构建与打包

在生产分支工作目录执行：

```powershell
cd backend
mvn test package
cd ../frontend
npm ci
npm run build
cd ..
./pack-production.ps1
```

打包脚本会将可部署文件放在 `artifacts/`，不包含密码、MySQL 数据、测试报告或 Git 元数据。构建环境需要 Java 17、Maven、Node.js 和 npm。

## 服务器内网验收

以下操作只在新目录执行。首次部署前确认 `39180` 未占用，并将 SSH 公钥授权给部署账号。

```bash
mkdir -p /DATA/QC_yangjichang/liugang_produce
cd /DATA/QC_yangjichang/liugang_produce
tar -xzf parking-produce-release.tar.gz
cp .env.production.example .env.production
chmod 600 .env.production
```

编辑 `.env.production`：填写两个不同的 MySQL 随机密码、首次平台管理员手机号及至少 12 位随机密码、物业入驻邀请码；将 `PROD_BIND_IP` 改为服务器实际内网 IP。不要提交或发送该文件。

```bash
docker compose --env-file .env.production \
  -f docker-compose.production.yml -f docker-compose.production.internal.yml \
  up -d --build
docker compose --env-file .env.production \
  -f docker-compose.production.yml -f docker-compose.production.internal.yml ps
curl -fsS http://192.168.0.147:39180/api/health
```

健康检查中的 IP 应与 `.env.production` 的 `PROD_BIND_IP` 一致。首次启动后确认平台管理员可登录，再从 `.env.production` 删除 `BOOTSTRAP_ADMIN_PASSWORD`，执行 `docker compose --env-file .env.production -f docker-compose.production.yml -f docker-compose.production.internal.yml up -d --force-recreate backend`；已有管理员会保留在数据库中。

验收顺序：平台管理员登录 → 使用邀请码注册物业与小区 → 确认空车位台账 → 物业录入真实车位 → 业主注册与申请 → 物业线下核验证明并审核 → 按顺位分配 → 物业凭合同及缴费凭据编号确认交付 → 检查审计记录核验。生产端不提供演示数据重置、固定短信码、自动人脸/OCR 或外部区块链认证。

## 切换 HTTPS 公网入口

运维提供匹配访问域名的有效证书，将证书链和私钥分别保存为 `certs/fullchain.pem`、`certs/privkey.pem`，限制私钥文件权限。确认 `39443` 已在服务器及防火墙放通后，在同一生产目录执行：

```bash
docker compose --env-file .env.production \
  -f docker-compose.production.yml -f docker-compose.production.https.yml \
  up -d --build
```

该配置仅映射 Nginx 的 `39443:443`，不再映射内网验收端口 `39180`。后端和 MySQL 均不对宿主机开放。公网地址为 `https://<已配置证书的域名>:39443/`。如证书或域名尚未就绪，仅保留内网验收，不处理真实居民数据。

## 数据与回退

- 生产 MySQL 数据保存在 `liugang_produce/mysql-data`；任何时候都不要复制测试端 `data/parking.db`。
- 每次升级前执行 `bash backup-production.sh` 备份 MySQL，并保留当前部署包。脚本使用 `mysqldump --single-transaction` 导出到受限的 `backups/` 目录。
- 回退应用时使用上一版本部署包重建生产容器；如果数据库结构已变化，先按备份恢复数据库。不要在测试目录执行生产端 Compose 命令。
- 当前首次空库部署由 Hibernate 创建表结构。后续结构升级必须引入受控迁移并在验收环境验证，不能依赖线上自动变更。
