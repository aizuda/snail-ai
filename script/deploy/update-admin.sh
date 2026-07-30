#!/usr/bin/env bash
# ============================================================
# Snail AI Admin 前端更新脚本
# 流程：安装依赖 → 构建 → 清理 nginx 旧包 → 拷贝新包
# ============================================================
set -euo pipefail

# ============================================================
# 配置区（按需修改）
# ============================================================
# 前端源码目录
FRONTEND_DIR="/workspace/snail-ai-admin"
# nginx 静态文件目录
NGINX_HTML_DIR="/workspace/snail-ai/script/docker/nginx/html/snailai-admin"
# 构建命令
BUILD_CMD="pnpm build:deploy"
# 安装命令
INSTALL_CMD="pnpm install --frozen-lockfile"
# ============================================================

if [ ! -d "$FRONTEND_DIR" ]; then
  echo "错误: 前端源码目录不存在: $FRONTEND_DIR"
  exit 1
fi

echo "========================================"
echo "  Snail AI Admin 前端更新"
echo "========================================"
echo "前端源码:  $FRONTEND_DIR"
echo "nginx 目录: $NGINX_HTML_DIR"
echo ""

# 1. 安装依赖
echo "[1/4] 安装依赖..."
cd "$FRONTEND_DIR"
$INSTALL_CMD

# 2. 构建
echo "[2/4] 构建前端..."
$BUILD_CMD

# 3. 清理 nginx 旧包
echo "[3/4] 清理旧包..."
rm -rf "$NGINX_HTML_DIR"
mkdir -p "$NGINX_HTML_DIR"

# 4. 拷贝新包
echo "[4/4] 拷贝新包..."
cp -r "$FRONTEND_DIR/dist/"* "$NGINX_HTML_DIR/"

echo ""
echo "完成! 访问: https://snailai-admin.opensnail.com/"