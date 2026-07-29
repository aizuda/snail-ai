#!/usr/bin/env bash
# ============================================================
# Snail AI — Ollama 模型管理脚本
# ============================================================
# 用途：管理 Ollama 本地模型服务，支持安装、启动、模型拉取。
#
# 使用方式：
#   bash scripts/ollama-setup.sh              # 检查状态并安装/启动 Ollama
#   bash scripts/ollama-setup.sh install      # 安装 Ollama
#   bash scripts/ollama-setup.sh start        # 启动 Ollama 服务
#   bash scripts/ollama-setup.sh pull         # 拉取配置的模型列表
#   bash scripts/ollama-setup.sh list         # 列出已下载的模型
#   bash scripts/ollama-setup.sh --help       # 显示帮助
# ============================================================
set -euo pipefail

# -------------------------------------------
# 颜色输出
# -------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}==>${NC} ${BLUE}$*${NC}"; }

# -------------------------------------------
# 配置
# -------------------------------------------
OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"

# 默认模型列表（可通过环境变量 OLLAMA_PULL_MODELS 覆盖）
# 格式: "model1:tag model2:tag"
DEFAULT_MODELS="${OLLAMA_PULL_MODELS:-qwen3:8b}"

# -------------------------------------------
# 帮助
# -------------------------------------------
usage() {
    cat << 'EOF'
用法: bash scripts/ollama-setup.sh [命令]

命令:
  (无参数)    检查状态，必要时安装并启动 Ollama
  install     安装 Ollama（Linux）
  start       启动 Ollama 服务
  pull        拉取环境变量 OLLAMA_PULL_MODELS 中配置的模型
  list        列出已下载的模型
  --help      显示帮助

环境变量:
  OLLAMA_HOST           Ollama 服务地址（默认: http://localhost:11434）
  OLLAMA_PULL_MODELS    启动时自动拉取的模型列表（空格分隔）

示例:
  bash scripts/ollama-setup.sh                    # 自动安装并启动
  OLLAMA_PULL_MODELS="qwen3:8b bge-m3:latest" bash scripts/ollama-setup.sh pull
EOF
}

# -------------------------------------------
# 检查 Ollama 服务是否运行
# -------------------------------------------
check_ollama_running() {
    if curl -s "${OLLAMA_HOST}/api/tags" >/dev/null 2>&1; then
        return 0
    fi
    return 1
}

# -------------------------------------------
# 安装 Ollama
# -------------------------------------------
install_ollama() {
    log_step "安装 Ollama"

    if command -v ollama >/dev/null 2>&1; then
        log_info "Ollama 已安装: $(ollama --version 2>&1 || echo 'unknown')"
        return 0
    fi

    local os_type
    os_type=$(uname -s)

    case "${os_type}" in
        Linux)
            log_info "检测到 Linux，使用官方脚本安装..."
            curl -fsSL https://ollama.com/install.sh | sh
            ;;
        *)
            log_error "不支持的操作系统: ${os_type}"
            log_error "请手动安装 Ollama: https://ollama.com/download"
            exit 1
            ;;
    esac

    log_info "Ollama 安装完成"
}

# -------------------------------------------
# 启动 Ollama 服务
# -------------------------------------------
start_ollama() {
    log_step "启动 Ollama 服务"

    if check_ollama_running; then
        log_info "Ollama 服务已在运行: ${OLLAMA_HOST}"
        return 0
    fi

    # 尝试通过 systemd 启动
    if systemctl is-enabled ollama >/dev/null 2>&1; then
        log_info "通过 systemd 启动 Ollama..."
        sudo systemctl start ollama
    elif command -v ollama >/dev/null 2>&1; then
        log_info "后台启动 Ollama..."
        nohup ollama serve > /tmp/ollama.log 2>&1 &
        sleep 3
    else
        log_error "Ollama 未安装，请先执行: bash scripts/ollama-setup.sh install"
        exit 1
    fi

    # 等待服务就绪
    local retries=10
    local count=0
    while [[ ${count} -lt ${retries} ]]; do
        if check_ollama_running; then
            log_info "Ollama 服务已启动: ${OLLAMA_HOST}"
            return 0
        fi
        sleep 1
        count=$((count + 1))
    done

    log_error "Ollama 启动超时，请检查日志: journalctl -u ollama 或 /tmp/ollama.log"
    return 1
}

# -------------------------------------------
# 拉取模型
# -------------------------------------------
pull_models() {
    log_step "拉取 Ollama 模型"

    if ! check_ollama_running; then
        log_warn "Ollama 服务未运行，正在启动..."
        start_ollama
    fi

    if [[ -z "${DEFAULT_MODELS}" ]]; then
        log_warn "未配置 OLLAMA_PULL_MODELS，跳过模型拉取"
        log_info "提示: 设置环境变量 OLLAMA_PULL_MODELS=\"model1 model2\" 来指定要拉取的模型"
        return 0
    fi

    for model in ${DEFAULT_MODELS}; do
        log_info "拉取模型: ${model} ..."
        if ollama pull "${model}"; then
            log_info "模型 ${model} 拉取完成"
        else
            log_error "模型 ${model} 拉取失败"
        fi
    done
}

# -------------------------------------------
# 列出模型
# -------------------------------------------
list_models() {
    log_step "已安装的模型"

    if ! check_ollama_running; then
        log_error "Ollama 服务未运行，无法获取模型列表"
        return 1
    fi

    ollama list
}

# -------------------------------------------
# 主流程
# -------------------------------------------
main() {
    local cmd="${1:-setup}"

    echo ""
    echo "============================================"
    echo "  Snail AI — Ollama 模型管理"
    echo "============================================"

    case "${cmd}" in
        --help|-h)
            usage
            exit 0
            ;;
        install)
            install_ollama
            ;;
        start)
            start_ollama
            ;;
        pull)
            pull_models
            ;;
        list)
            list_models
            ;;
        setup|"")
            install_ollama
            start_ollama
            pull_models
            list_models
            ;;
        *)
            log_error "未知命令: ${cmd}"
            usage
            exit 1
            ;;
    esac
}

main "$@"
