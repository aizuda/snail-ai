#!/usr/bin/env bash
# ============================================================
# Snail AI Server 更新脚本
# 流程：Maven 构建 → 停容器 → 删镜像 → 重建启动
# ============================================================
set -euo pipefail

# ============================================================
# 配置区（按需修改）
# ============================================================
# 服务器源码目录
PROJECT_DIR="/workspace/snail-ai"
# docker-compose.yml 所在目录
COMPOSE_DIR="/workspace/snail-ai/script/docker"
# 服务名
SERVICE_NAME="snail-ai-server"
# 镜像名
IMAGE_NAME="snail-ai-server:latest"
# Maven 构建参数
MVN_OPTS="-DskipTests -T 1C"
# ============================================================

COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"

if [ ! -d "$PROJECT_DIR" ]; then
  echo "错误: 源码目录不存在: $PROJECT_DIR"
  exit 1
fi
if [ ! -f "$COMPOSE_FILE" ]; then
  echo "错误: docker-compose.yml 不存在: $COMPOSE_FILE"
  exit 1
fi

echo "========================================"
echo "  Snail AI Server 更新"
echo "========================================"
echo "源码:  $PROJECT_DIR"
echo "compose: $COMPOSE_FILE"
echo ""

# 1. Maven 构建
echo "[1/4] Maven 构建..."
cd "$PROJECT_DIR"
mvn clean package $MVN_OPTS

# 2. 停容器
echo "[2/4] 停止容器..."
cd "$COMPOSE_DIR"
docker compose stop "$SERVICE_NAME" 2>/dev/null || true

# 3. 删镜像
echo "[3/4] 删除旧镜像..."
docker rmi "$IMAGE_NAME" 2>/dev/null || true

# 4. 重建启动
echo "[4/4] 重建并启动..."
docker compose up -d --build "$SERVICE_NAME"

echo ""
echo "完成! 查看日志: docker compose -f $COMPOSE_FILE logs -f $SERVICE_NAME"