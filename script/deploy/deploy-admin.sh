#!/usr/bin/env bash
# ============================================
# Snail AI Admin 前端部署脚本
# 构建前端项目并拷贝到 nginx 静态文件目录
# ============================================
set -euo pipefail

# ============================================
# 配置区（按需修改）
# ============================================

# 前端源码目录
FRONTEND_SRC="/workspace/snail-ai-admin"

# 部署输出目录（nginx 静态文件目录）
DEPLOY_TARGET="/docker/nginx/html/snailai-admin"

# Docker nginx 目录（用于提示重启命令）
NGINX_DIR="/docker"

# 构建命令（仅构建前端 dist/，不涉及 Java 后端）
BUILD_CMD="pnpm build:deploy"

# 包管理器安装命令
INSTALL_CMD="pnpm install --frozen-lockfile"

# ============================================
# 脚本逻辑（一般无需修改）
# ============================================

# Windows 路径转换（Git Bash）
to_unix_path() {
  echo "$1" | sed 's|\\|/|g' | sed 's|^\([A-Za-z]\):|/\1|'
}

FRONTEND_SRC=$(to_unix_path "$FRONTEND_SRC")
DEPLOY_TARGET=$(to_unix_path "$DEPLOY_TARGET")
NGINX_DIR=$(to_unix_path "$NGINX_DIR")

# 校验
if [ ! -d "$FRONTEND_SRC" ]; then
  echo "❌ 前端源码目录不存在：$FRONTEND_SRC"
  echo "   请修改脚本顶部 FRONTEND_SRC 配置"
  exit 1
fi

echo "============================================"
echo "  Snail AI Admin 前端部署"
echo "============================================"
echo "  源码目录：$FRONTEND_SRC"
echo "  部署目标：$DEPLOY_TARGET"
echo "  nginx 目录：$NGINX_DIR"
echo ""

# 1. 安装依赖
echo "[1/3] 安装依赖..."
cd "$FRONTEND_SRC"
$INSTALL_CMD

# 2. 构建
echo "[2/3] 构建生产包..."
$BUILD_CMD

# 3. 拷贝到部署目录
echo "[3/3] 拷贝到部署目录..."
rm -rf "$DEPLOY_TARGET"
mkdir -p "$DEPLOY_TARGET"
cp -r "$FRONTEND_SRC/dist/"* "$DEPLOY_TARGET/"

echo ""
echo "✅ 部署完成！"
echo "  访问地址：https://snailai-admin.opensnail.com/"
echo ""
echo "  如需重启 nginx："
echo "    cd $NGINX_DIR && docker compose restart nginx"