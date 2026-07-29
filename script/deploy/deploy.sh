#!/usr/bin/env bash
# ============================================================
# Snail AI — CI/CD 部署脚本
# ============================================================
# 用途：从源码构建 Snail AI Server 和 Agent Example，
#       通过 Docker Compose 一键启动。
#
# 使用方式：
#   bash script/deploy/deploy.sh              # 完整流程：构建 + 部署
#   bash script/deploy/deploy.sh build        # 仅 Maven 构建 + Docker 镜像构建
#   bash script/deploy/deploy.sh deploy       # 仅 docker compose up -d
#   bash script/deploy/deploy.sh restart      # 重启后端服务
#   bash script/deploy/deploy.sh status       # 查看服务状态
#   bash script/deploy/deploy.sh logs         # 查看后端日志
#   bash script/deploy/deploy.sh init-db      # 初始化数据库
#   bash script/deploy/deploy.sh --help       # 显示帮助
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

# ----- 可覆盖配置 -----
PROJECT_DIR="${SNAIL_AI_PROJECT_DIR:-${REPO_ROOT}}"
COMPOSE_DIR="${SNAIL_AI_COMPOSE_DIR:-${PROJECT_DIR}/script/docker}"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"
HEALTH_URL="${SNAIL_AI_HEALTH_URL:-http://localhost:8900/snail-ai/actuator/health}"
HEALTH_RETRIES="${SNAIL_AI_HEALTH_RETRIES:-30}"
HEALTH_INTERVAL="${SNAIL_AI_HEALTH_INTERVAL:-3}"
MVN_OPTS="${SNAIL_AI_MVN_OPTS:--DskipTests -T 1C}"
GIT_BRANCH="${SNAIL_AI_GIT_BRANCH:-main}"

# ----- 帮助 -----
show_help() {
  cat <<EOF
Snail AI CI/CD 部署脚本

用法: bash script/deploy/deploy.sh [命令]

命令:
  (无参数)       完整流程：Git 拉取 → Maven 构建 → Docker 构建 → 部署
  build          仅 Maven 构建 + Docker 镜像构建
  deploy         仅 docker compose up -d
  restart        重启 Snail AI Server + Agent Example
  update         拉取最新代码并重新构建部署
  status         查看所有服务运行状态
  logs           查看后端日志
  health         执行健康检查
  init-db        初始化数据库（首次部署后执行）
  --help         显示此帮助

环境变量（可写入 script/deploy/deploy.env）:
  SNAIL_AI_PROJECT_DIR    项目源码目录（默认: 脚本所在仓库根目录）
  SNAIL_AI_COMPOSE_DIR    docker-compose.yml 所在目录
  SNAIL_AI_GIT_BRANCH     Git 分支（默认: main）
  SNAIL_AI_MVN_OPTS       Maven 构建参数
  SNAIL_AI_HEALTH_URL     健康检查地址
EOF
  exit 0
}

# ----- 加载本地配置 -----
load_env() {
  local env_file="${PROJECT_DIR}/script/deploy/deploy.env"
  if [ -f "$env_file" ]; then
    set -a; source "$env_file"; set +a
    log_info "已加载: $env_file"
  fi
}

# ----- 环境检查 -----
check_env() {
  log_step "检查构建环境"

  local missing=()

  command -v java  >/dev/null 2>&1 || missing+=("java (JDK 21)")
  command -v mvn   >/dev/null 2>&1 || missing+=("maven")
  command -v git   >/dev/null 2>&1 || missing+=("git")
  command -v docker >/dev/null 2>&1 || missing+=("docker")

  if [ ${#missing[@]} -gt 0 ]; then
    log_error "缺少依赖: ${missing[*]}"
    log_info "请先运行: sudo bash script/ci-setup.sh"
    exit 1
  fi

  if ! docker compose version >/dev/null 2>&1; then
    log_error "需要 Docker Compose V2 (docker compose)"
    exit 1
  fi

  log_info "环境检查通过"
}

# ----- Git 拉取 -----
git_pull() {
  log_step "拉取最新代码"
  cd "$PROJECT_DIR"

  git fetch origin
  git checkout "$GIT_BRANCH"
  git pull origin "$GIT_BRANCH"

  log_info "当前提交: $(git log -1 --oneline)"
}

# ----- Maven 构建 -----
maven_build() {
  log_step "Maven 构建"
  cd "$PROJECT_DIR"

  mvn clean package $MVN_OPTS

  log_info "Maven 构建完成"
}

# ----- Docker 构建 -----
docker_build() {
  log_step "Docker 镜像构建"
  cd "$COMPOSE_DIR"

  docker compose -f "$COMPOSE_FILE" build \
    snail-ai-server \
    snail-ai-agent-example

  log_info "Docker 镜像构建完成"
}

# ----- 部署 -----
deploy() {
  log_step "启动服务"
  cd "$COMPOSE_DIR"

  docker compose -f "$COMPOSE_FILE" up -d

  log_info "服务已启动"
}

# ----- 重启后端 -----
restart() {
  log_step "重启后端服务"
  cd "$COMPOSE_DIR"

  docker compose -f "$COMPOSE_FILE" up -d --force-recreate \
    snail-ai-server \
    snail-ai-agent-example

  log_info "后端服务已重启"
}

# ----- 数据库初始化 -----
init_db() {
  log_step "初始化数据库"
  bash "${SCRIPT_DIR}/init-db.sh"
}

# ----- 状态 -----
status() {
  log_step "服务状态"
  cd "$COMPOSE_DIR"
  docker compose -f "$COMPOSE_FILE" ps
}

# ----- 日志 -----
logs() {
  cd "$COMPOSE_DIR"
  docker compose -f "$COMPOSE_FILE" logs -f --tail=100 snail-ai-server snail-ai-agent-example
}

# ----- 健康检查 -----
health_check() {
  log_step "健康检查: $HEALTH_URL"
  for i in $(seq 1 "$HEALTH_RETRIES"); do
    if curl -fs "$HEALTH_URL" >/dev/null 2>&1; then
      log_info "健康检查通过 (尝试 $i/$HEALTH_RETRIES)"
      return 0
    fi
    sleep "$HEALTH_INTERVAL"
  done
  log_error "健康检查失败"
  return 1
}

# ----- 完整流程 -----
full_deploy() {
  check_env
  git_pull
  maven_build
  docker_build
  deploy
  health_check
}

# ----- 更新流程 -----
update() {
  check_env
  git_pull
  maven_build
  docker_build
  restart
  health_check
}

# ============================================================
# 入口
# ============================================================
load_env

case "${1:-}" in
  --help|-h) show_help ;;
  build)     check_env && maven_build && docker_build ;;
  deploy)    deploy ;;
  restart)   restart ;;
  update)    update ;;
  status)    status ;;
  logs)      logs ;;
  health)    health_check ;;
  init-db)   init_db ;;
  "")        full_deploy ;;
  *)
    log_error "未知命令: $1"
    show_help
    ;;
esac