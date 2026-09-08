# ==========================================================
# 慧停管家 · 单容器一体化部署 Dockerfile (All-in-One)
# 前端 Vite 编译 + Python 3.11 FastAPI + Nginx 反向代理
# ==========================================================

# 阶段 1: 前端静态构建
FROM node:20-alpine AS frontend-builder
WORKDIR /build
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# 阶段 2: 整合 Python 后端与 Nginx 网关
FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    DATABASE_URL=sqlite:////app/data/parking.db

# 安装系统工具、Nginx 与健康检查依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    nginx \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 安装后端依赖
COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制后端源码
COPY backend/ .

# 创建持久化数据目录
RUN mkdir -p /app/data

# 复制前端打包产物至 Nginx 目录
COPY --from=frontend-builder /build/dist /usr/share/nginx/html

# 配置 Nginx 单容器站点规则
COPY docker/nginx-single.conf /etc/nginx/sites-available/default
RUN rm -f /etc/nginx/sites-enabled/default && ln -s /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default

# 复制启动脚本
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 暴露端口 (80 为前台与反代统一入口，8000 为后端调试备用)
EXPOSE 80 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

CMD ["/app/entrypoint.sh"]
