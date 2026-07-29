#!/usr/bin/env bash
# ============================================================
# Snail AI — 数据库初始化脚本
# ============================================================
# 用途：MySQL 容器启动后，执行建表 SQL。
#
# 前提：docker compose up -d mysql 已运行且 healthy
#
# 使用方式：
#   bash script/deploy/init-db.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="${SCRIPT_DIR}/../sql"

MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-root}"
MYSQL_DB="${MYSQL_DB:-snail_ai}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}==>${NC} ${BLUE}$*${NC}"; }

# 检查 MySQL 是否就绪
log_step "检查 MySQL 容器状态"
if ! docker exec "$MYSQL_CONTAINER" mysqladmin ping -h 127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASS" --silent 2>/dev/null; then
  log_error "MySQL 容器未就绪，请先启动: docker compose -f script/docker/docker-compose.yml up -d mysql"
  exit 1
fi
log_info "MySQL 已就绪"

# 执行建表
log_step "执行建表脚本"
for f in "$SQL_DIR"/snail_ai_schema.sql; do
  if [ -f "$f" ]; then
    log_info "执行: $(basename "$f")"
    docker exec -i "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" "$MYSQL_DB" < "$f"
  fi
done

log_info "数据库初始化完成"