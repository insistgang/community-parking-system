# ==========================================
# 慧停管家 · 后端 FastAPI 服务 Dockerfile
# ==========================================
FROM python:3.11-slim

WORKDIR /app

# 设置环境变量，提高 Python 运行性能与日志实时性
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PORT=8000

# 安装运行基础工具 (curl 供健康检查)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 先复制依赖文件并安装 (利用 Docker 镜像缓存)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制后端业务源码
COPY . .

# 创建持久化数据目录
RUN mkdir -p /app/data

# 暴露 FastAPI 端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/ || exit 1

# 启动命令
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
