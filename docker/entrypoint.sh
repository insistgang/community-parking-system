#!/bin/bash
set -e

echo "=========================================================="
echo "    🚀 慧停管家 · 老旧小区停车位综合管理平台 (Docker 容器)"
echo "=========================================================="

# 捕获退出信号并优雅关闭后台子进程
trap "echo '收到停止信号，正在停止所有服务...'; kill -TERM 0" SIGINT SIGTERM EXIT

# 启动 FastAPI 后端
echo ">>> [1/2] 正在启动 FastAPI 后端服务 (端口 8000)..."
uvicorn main:app --host 127.0.0.1 --port 8000 &
BACKEND_PID=$!

# 等待后端就绪
sleep 2

# 启动 Nginx 前端网关
echo ">>> [2/2] 正在启动 Nginx 静态服务与反向代理网关 (端口 80)..."
nginx -g "daemon off;" &
NGINX_PID=$!

echo "✅ 容器已就绪！服务已全面监听端口 80 与 8000。"

wait
