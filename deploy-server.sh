#!/usr/bin/env bash
# ==========================================================
# 慧停管家 · 一键同步并部署到远程测试服务器 (192.168.0.147)
# ==========================================================
set -e

SERVER_HOST="192.168.0.147"
SERVER_PORT="1024"
SERVER_USER="qc_yangjichang"
SERVER_DIR="/DATA/QC_yangjichang/liugang_xiaoliu"

echo "==> 1. 推送本地代码到 GitHub..."
git push origin main

echo "==> 2. 连接服务器 ($SERVER_HOST:$SERVER_PORT) 并更新部署..."
ssh -p "$SERVER_PORT" "${SERVER_USER}@${SERVER_HOST}" "cd $SERVER_DIR && git pull && bash docker-deploy.sh"

echo "=========================================================="
echo "🎉 服务器部署更新完成！"
echo "👉 访问入口: http://${SERVER_HOST}:39080/"
echo "=========================================================="
