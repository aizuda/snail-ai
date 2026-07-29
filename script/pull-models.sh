#!/usr/bin/env bash
# ============================================================
# Ollama 模型拉取脚本 — Snail AI 演示环境
# ============================================================
# 用途：Ollama 容器启动后，拉取最小可用的 Embedding 和 Rerank 模型。
#
# 前置条件：docker compose up -d ollama 已运行
#
# 使用方式：
#   bash script/pull-models.sh
#   # 或指定容器名：
#   OLLAMA_CONTAINER=my-ollama bash script/pull-models.sh
# ============================================================
set -euo pipefail

OLLAMA_CONTAINER="${OLLAMA_CONTAINER:-ollama}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[OK]${NC}  $*"; }
log_step()  { echo -e "\n${BLUE}==>${NC} ${BLUE}$*${NC}"; }
log_warn()  { echo -e "${RED}[!!]${NC} $*"; }

# 检查 Ollama 是否就绪
log_step "检查 Ollama 容器状态"
if ! docker exec "$OLLAMA_CONTAINER" ollama list >/dev/null 2>&1; then
  log_warn "Ollama 容器未就绪，请先启动: docker compose -f script/docker/docker-compose.yml up -d ollama"
  exit 1
fi
log_info "Ollama 已就绪"

# 拉取 Embedding 模型（最小向量模型 ~274MB）
log_step "拉取 Embedding 模型: nomic-embed-text"
docker exec "$OLLAMA_CONTAINER" ollama pull nomic-embed-text
log_info "nomic-embed-text 拉取完成"

# 可选：最小聊天模型（~352MB），需要时取消注释
# log_step "拉取聊天模型: qwen2:0.5b"
# docker exec "$OLLAMA_CONTAINER" ollama pull qwen2:0.5b
# log_info "qwen2:0.5b 拉取完成"

echo ""
log_info "模型拉取完成。当前已安装模型："
docker exec "$OLLAMA_CONTAINER" ollama list