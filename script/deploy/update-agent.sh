#!/usr/bin/env bash
# ============================================================
# Snail AI Agent Example 更新脚本
# ============================================================
# 流程：Maven 构建 → 停容器 → 删镜像 → 重建启动
# 使用方式：
#   bash script/deploy/update-agent.sh
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
PROJECT_DIR="${SNAIL_AI_PROJECT_DIR:-${REPO_ROOT}}"
COMPOSE_DIR="${SNAIL_AI_COMPOSE_DIR:-${PROJECT_DIR}/script/docker}"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"
AGENT_MODULE="${PROJECT_DIR}/snail-ai-agent-example"
SERVICE_NAME="snail-ai-agent-example"
IMAGE_NAME="snail-ai-agent-example:latest"
MVN_OPTS="${SNAIL_AI_MVN_OPTS:--DskipTests -T 1C}"

# ----- 校验 -----
if [ ! -d "$PROJECT_DIR" ]; then
  log_error "源码目录不存在: $PROJECT_DIR"
  exit 1
fi
if [ ! -d "$AGENT_MODULE" ]; then
  log_error "Agent 模块目录不存在: $AGENT_MODULE"
  exit 1
fi
if [ ! -f "$COMPOSE_FILE" ]; then
  log_error "docker-compose.yml 不存在: $COMPOSE_FILE"
  exit 1
fi

echo "============================================"
echo "  Snail AI Agent Example 更新"
echo "============================================"
echo "  源码目录:  $PROJECT_DIR"
echo "  Agent 模块: $AGENT_MODULE"
echo "  compose:   $COMPOSE_FILE"
echo "  服务:      $SERVICE_NAME"
echo ""

# ============================================================
# 1. Maven 构建
# ============================================================
log_step "[1/4] Maven 构建 Agent Example"
cd "$AGENT_MODULE"
mvn clean package $MVN_OPTS
log_info "Maven 构建完成"

# ============================================================
# 2. 停容器
# ============================================================
log_step "[2/4] 停止容器"
cd "$COMPOSE_DIR"
docker compose -f "$COMPOSE_FILE" stop "$SERVICE_NAME" || true
log_info "容器已停止"

# ============================================================
# 3. 删镜像
# ============================================================
log_step "[3/4] 删除旧镜像"
if docker images -q "$IMAGE_NAME" 2>/dev/null | grep -q .; then
  docker rmi "$IMAGE_NAME" || true
  log_info "旧镜像已删除"
else
  log_info "无旧镜像，跳过"
fi

# ============================================================
# 4. 重建启动
# ============================================================
log_step "[4/4] 重建并启动"
docker compose -f "$COMPOSE_FILE" up -d --build "$SERVICE_NAME"
log_info "服务已启动"

echo ""
echo "============================================"
echo "  Snail AI Agent Example 更新完成"
echo "============================================"
echo "  查看日志: docker compose -f $COMPOSE_FILE logs -f $SERVICE_NAME"
echo ""