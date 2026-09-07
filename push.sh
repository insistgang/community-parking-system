#!/usr/bin/env bash
# 老旧小区停车位系统 - 一键提交与推送 GitHub 脚本
set -e

MSG=${1:-"update: 同步最新系统代码与文档变更"}

echo "==> 1. 检查并暂存所有工作区变更..."
git add .

if git diff-index --quiet HEAD --; then
    echo "==> 工作区无未提交变更，直接推送..."
else
    echo "==> 2. 正在提交变更: '$MSG'..."
    git commit -m "$MSG"
fi

echo "==> 3. 正在推送到 GitHub 私有仓库 (insistgang/community-parking-system)..."
git push origin main

echo "========================================================"
echo "✅ 推送成功！最新代码已同步到 GitHub 私有仓库："
echo "   https://github.com/insistgang/community-parking-system"
echo "========================================================"
