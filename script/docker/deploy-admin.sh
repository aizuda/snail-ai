#!/usr/bin/env bash
# ============================================
# Snail AI Admin 前端部署脚本
# 构建前端项目并拷贝到 nginx 静态文件目录
# ============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
NGINX_HTML="$PROJECT_ROOT/script/docker/nginx/html/snailai-admin"

# 从 .env 读取前端项目路径
ENV_FILE="$PROJECT_ROOT/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "❌ 未找到 .env 文件：$ENV_FILE"
  exit 1
fi

FRONTEND_PATH=$(grep -E '^SNAIL_AI_FRONTEND_PATH=' "$ENV_FILE" | head -1 | sed 's/^SNAIL_AI_FRONTEND_PATH=//' | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

if [ -z "$FRONTEND_PATH" ]; then
  echo "❌ .env 中未配置 SNAIL_AI_FRONTEND_PATH"
  exit 1
fi

# Windows 路径转换（Git Bash）
FRONTEND_PATH=$(echo "$FRONTEND_PATH" | sed 's|\\|/|g' | sed 's|^\([A-Za-z]\):|/\1|')

if [ ! -d "$FRONTEND_PATH" ]; then
  echo "❌ 前端项目路径不存在：$FRONTEND_PATH"
  exit 1
fi

echo "============================================"
echo "  Snail AI Admin 前端部署"
echo "============================================"
echo "  前端项目：$FRONTEND_PATH"
echo "  部署目标：$NGINX_HTML"
echo ""

# 1. 安装依赖
echo "[1/3] 安装依赖..."
cd "$FRONTEND_PATH"
pnpm install --frozen-lockfile

# 2. 构建
echo "[2/3] 构建生产包..."
pnpm build

# 3. 拷贝到 nginx html
echo "[3/3] 拷贝到 nginx 静态目录..."
rm -rf "$NGINX_HTML"
mkdir -p "$NGINX_HTML"
cp -r "$FRONTEND_PATH/dist/"* "$NGINX_HTML/"

echo ""
echo "✅ 部署完成！"
echo "  访问地址：https://snailai-admin.opensnail.com/snail-ai/"
echo ""
echo "  如需重启 nginx："
echo "    cd $PROJECT_ROOT/script/docker && docker compose restart nginx"