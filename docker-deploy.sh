#!/usr/bin/env bash
# ==========================================================
# 慧停管家 · 服务器 Docker 极速一键部署脚本
# ==========================================================
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "=========================================================="
echo "    🚀 慧停管家 · 老旧小区停车位综合管理平台 Docker 部署"
echo "=========================================================="

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: 未检测到 Docker，请先在服务器上安装 Docker。"
    echo "   Ubuntu/Debian 快捷安装: curl -fsSL https://get.docker.com | bash"
    exit 1
fi

# 创建宿主机持久化数据目录 (确保数据库断电不丢失)
mkdir -p data

# 判断是否有 Compose 工具
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    COMPOSE_CMD=""
fi

if [ -n "$COMPOSE_CMD" ]; then
    echo ">>> 检测到 $COMPOSE_CMD，正在构建并启动多容器微服务架构..."
    $COMPOSE_CMD up -d --build
else
    echo ">>> 未检测到 Docker Compose，自动采用单容器一体化模式构建部署..."
    docker build -t community-parking .
    docker stop parking-app 2>/dev/null || true
    docker rm parking-app 2>/dev/null || true
    docker run -d \
        --name parking-app \
        --restart always \
        -p ${PORT:-80}:80 \
        -p ${BACKEND_PORT:-8000}:8000 \
        -v "$DIR/data:/app/data" \
        community-parking
fi

echo "=========================================================="
echo "🎉 部署成功！系统已在后台稳定运行。"
echo "📱 业主端与物业中台访问地址: http://<服务器公网或局域网IP>:${PORT:-80}/"
echo "📚 Swagger API 接口文档: http://<服务器公网或局域网IP>:${PORT:-80}/docs"
echo "💾 SQLite 数据库持久化目录: $DIR/data/parking.db"
echo "=========================================================="
