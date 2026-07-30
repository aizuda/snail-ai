#!/usr/bin/env bash
# ============================================================
# Snail AI Admin 前端更新脚本
# ============================================================
# 流程：前端构建 → 清理 nginx 旧包 → 拷贝新包
# 使用方式：
#   bash script/deploy/update-admin.sh
# ============================================================
set -euo pipefail

# ----- 颜色 -----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}==>${NC} ${BLUE}$*${NC}"; }

# ----- 路径 -----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# ----- 可配置路径（优先使用环境变量）-----
FRONTEND_DIR="${SNAIL_AI_FRONTEND_DIR:-/workspace/snail-ai-admin}"
NGINX_HTML_DIR="${SNAIL_AI_NGINX_HTML_DIR:-/docker/nginx/html/snailai-admin}"
BUILD_CMD="pnpm build:deploy"
INSTALL_CMD="pnpm install --frozen-lockfile"

# ----- 校验 -----
if [ ! -d "$FRONTEND_DIR" ]; then
  log_error "前端源码目录不存在: $FRONTEND_DIR"
  log_info "请设置 SNAIL_AI_FRONTEND_DIR 环境变量"
  exit 1
fi

echo "============================================"
echo "  Snail AI Admin 前端更新"
echo "============================================"
echo "  前端源码:  $FRONTEND_DIR"
echo "  nginx 目录: $NGINX_HTML_DIR"
echo ""

# ============================================================
# 1. 安装依赖
# ============================================================
log_step "[1/4] 安装依赖"
cd "$FRONTEND_DIR"
$INSTALL_CMD
log_info "依赖安装完成"

# ============================================================
# 2. 构建
# ============================================================
log_step "[2/4] 构建前端"
$BUILD_CMD
log_info "前端构建完成"

# ============================================================
# 3. 清理 nginx 旧包
# ============================================================
log_step "[3/4] 清理 nginx 旧包"
rm -rf "$NGINX_HTML_DIR"
mkdir -p "$NGINX_HTML_DIR"
log_info "旧包已清理"

# ============================================================
# 4. 拷贝新包
# ============================================================
log_step "[4/4] 拷贝新包到 nginx 目录"
cp -r "$FRONTEND_DIR/dist/"* "$NGINX_HTML_DIR/"
log_info "拷贝完成"

echo ""
echo "============================================"
echo "  Snail AI Admin 前端更新完成"
echo "============================================"
echo "  访问地址: https://snailai-admin.opensnail.com/"
echo ""