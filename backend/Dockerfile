# ==========================================
# 慧停管家 · 后端 FastAPI 服务 Dockerfile
# ==========================================
FROM python:3.11-slim

WORKDIR /app

# 设置环境变量，提高 Python 运行性能与日志实时性
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PORT=8000

# 先复制依赖文件并利用清华镜像源极速安装 (避免海外源网络超时)
COPY requirements.txt .
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt

# 复制后端业务源码
COPY . .

# 创建持久化数据目录
RUN mkdir -p /app/data

# 暴露 FastAPI 端口
EXPOSE 8000

# 健康检查 (使用 Python 原生 urllib，免 apt 安装外部工具)
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/')" || exit 1

# 启动命令
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
