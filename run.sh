#!/usr/bin/env bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PARKING_BIND_HOST="${PARKING_HOST:-127.0.0.1}"
cd "$DIR"

echo "========================================================"
echo "    老旧小区停车位综合管理系统 (全栈一键启动脚本)"
echo "    技术栈: Vue 3 + Tailwind CSS + FastAPI + SQLite"
echo "    专供: 2026-09-09 向张总汇报评审演示"
echo "========================================================"

# 1. 启动后端服务
echo ">>> [1/3] 正在准备后端环境与数据库..."
cd backend
if [ ! -d "venv" ]; then
    echo "创建 Python 虚拟环境..."
    python3 -m venv venv
    venv/bin/pip install -r requirements.txt -q
fi
if ! venv/bin/python --version >/dev/null 2>&1; then
    echo "后端虚拟环境解释器已失效；请先重建 backend/venv，再启动。" >&2
    exit 1
fi

# 初始化/检查数据
venv/bin/python seed.py

echo ">>> [2/3] 正在启动 FastAPI 后端服务 (端口: 8000)..."
venv/bin/uvicorn main:app --host "$PARKING_BIND_HOST" --port 8000 > backend.log 2>&1 &
BACKEND_PID=$!
cd "$DIR"

# 等待后端启动
sleep 2

# 2. 启动前端服务
echo ">>> [3/3] 正在启动 Vue 3 + Vite 前端服务 (端口: 5173)..."
cd frontend
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi

npm run dev -- --host "$PARKING_BIND_HOST" --port 5173 > frontend.log 2>&1 &
FRONTEND_PID=$!
cd "$DIR"

sleep 2

echo "========================================================"
echo "    🎉 系统已成功启动完毕！"
echo "    📱 业主移动端 H5 访问地址: http://localhost:5173/"
echo "    🏢 物业管理中台 PC 访问地址: http://localhost:5173/property/dashboard"
echo "    📚 后端 Swagger API 接口文档: http://localhost:8000/docs"
echo "========================================================"
echo "    提示: 按 Ctrl + C 可同时停止前后端所有服务。"
echo "========================================================"

trap "echo '正在停止服务...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" SIGINT SIGTERM

wait
