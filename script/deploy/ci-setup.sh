#!/usr/bin/env bash
# ============================================================
# Snail AI — CI/CD 构建环境安装脚本
# ============================================================
# 用途：在 Linux 宿主机上安装 JDK 21 + Maven + Git + Node.js 24 + pnpm
#       为 update-*.sh 提供编译构建环境。
#
# 支持：Ubuntu 20.04+ / Debian 11+ / CentOS 7+ / RHEL 8+
#
# 使用方式：
#   bash script/ci-setup.sh              # 安装全部
#   bash script/ci-setup.sh jdk          # 仅安装 JDK 21
#   bash script/ci-setup.sh maven        # 仅安装 Maven
#   bash script/ci-setup.sh git          # 仅安装 Git
#   bash script/ci-setup.sh node         # 仅安装 Node.js 24 + pnpm
#   bash script/ci-setup.sh maven-mirror # 仅配置 Maven 镜像
#   bash script/ci-setup.sh --help       # 显示帮助
# ============================================================
set -euo pipefail

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_step() { echo -e "\n${BLUE}==>${NC} ${BLUE}$*${NC}"; }

# ----- 检测发行版 -----
detect_os() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS="${ID}"
    OS_VERSION="${VERSION_ID}"
  else
    log_info "无法检测发行版，尝试通用方式"
    OS="unknown"
  fi
}

# ----- 安装 JDK 21 (BellSoft Liberica) -----
install_jdk() {
  log_step "安装 JDK 21 (BellSoft Liberica)"

  if command -v java >/dev/null 2>&1; then
    local jver
    jver=$(java -version 2>&1 | head -1)
    log_info "已安装 Java: $jver"
    # 即使已安装，也确保 JAVA_HOME 已设置
    if [ -z "${JAVA_HOME:-}" ]; then
      local java_home
      java_home=$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")
      cat > /etc/profile.d/snail-java.sh <<EOF
export JAVA_HOME=${java_home}
export PATH=\$JAVA_HOME/bin:\$PATH
EOF
      chmod 644 /etc/profile.d/snail-java.sh
      export JAVA_HOME="${java_home}"
      export PATH="${java_home}/bin:${PATH}"
      log_info "JAVA_HOME=${java_home}"
    fi
    return 0
  fi

  case "$OS" in
    ubuntu|debian)
      apt-get update -qq
      apt-get install -y -qq wget gnupg
      wget -qO - https://download.bell-sw.com/pki/GPG-KEY-bellsoft | gpg --dearmor > /etc/apt/trusted.gpg.d/bellsoft.gpg
      echo "deb [arch=$(dpkg --print-architecture)] https://apt.bell-sw.com/ stable main" > /etc/apt/sources.list.d/bellsoft.list
      apt-get update -qq
      apt-get install -y -qq bellsoft-java21
      ;;
    centos|rhel|fedora|rocky|almalinux|alinux)
      rpm --import https://download.bell-sw.com/pki/GPG-KEY-bellsoft
      cat > /etc/yum.repos.d/bellsoft.repo <<'REPO'
[BellSoft]
name=BellSoft Repository
baseurl=https://yum.bell-sw.com
enabled=1
gpgcheck=1
gpgkey=https://download.bell-sw.com/pki/GPG-KEY-bellsoft
REPO
      yum install -y bellsoft-java21 2>/dev/null || dnf install -y bellsoft-java21
      ;;
    *)
      log_info "请手动安装 BellSoft Liberica JDK 21: https://bell-sw.com/pages/downloads/"
      return 1
      ;;
  esac

  # 设置 JAVA_HOME 到系统环境，确保 Maven 等工具使用正确的 JDK
  local java_home
  java_home=$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")
  cat > /etc/profile.d/snail-java.sh <<EOF
export JAVA_HOME=${java_home}
export PATH=\$JAVA_HOME/bin:\$PATH
EOF
  chmod 644 /etc/profile.d/snail-java.sh
  # 立即生效
  export JAVA_HOME="${java_home}"
  export PATH="${java_home}/bin:${PATH}"

  log_info "JDK 21 安装完成: $(java -version 2>&1 | head -1)"
  log_info "JAVA_HOME=${java_home}"
}

# ----- 安装 Maven -----
install_maven() {
  log_step "安装 Maven"

  if command -v mvn >/dev/null 2>&1; then
    log_info "已安装 Maven: $(mvn --version 2>&1 | head -1)"
    return 0
  fi

  case "$OS" in
    ubuntu|debian)
      apt-get update -qq
      apt-get install -y -qq maven
      ;;
    centos|rhel|fedora|rocky|almalinux|alinux)
      # 不通过系统仓库安装 Maven，会拖入旧版 JDK 依赖
      local mvn_ver="3.9.16"
      local mvn_url="https://dlcdn.apache.org/maven/maven-3/${mvn_ver}/binaries/apache-maven-${mvn_ver}-bin.tar.gz"
      log_info "下载 Maven ${mvn_ver} ..."
      curl -fSL "$mvn_url" -o /tmp/maven.tar.gz
      tar -xzf /tmp/maven.tar.gz -C /usr/local/
      ln -sf /usr/local/apache-maven-${mvn_ver}/bin/mvn /usr/local/bin/mvn
      rm -f /tmp/maven.tar.gz
      ;;
    *)
      # 通用：手动下载解压
      local mvn_ver="3.9.16"
      local mvn_url="https://dlcdn.apache.org/maven/maven-3/${mvn_ver}/binaries/apache-maven-${mvn_ver}-bin.tar.gz"
      log_info "下载 Maven ${mvn_ver} ..."
      curl -fSL "$mvn_url" -o /tmp/maven.tar.gz
      tar -xzf /tmp/maven.tar.gz -C /usr/local/
      ln -sf /usr/local/apache-maven-${mvn_ver}/bin/mvn /usr/local/bin/mvn
      rm -f /tmp/maven.tar.gz
      ;;
  esac

  # 清除 bash 命令缓存，避免旧 /usr/bin/mvn 路径残留
  hash -r 2>/dev/null || true

  log_info "Maven 安装完成: $(mvn --version 2>&1 | head -1)"
}

# ----- 安装 Git -----
install_git() {
  log_step "安装 Git"

  if command -v git >/dev/null 2>&1; then
    log_info "已安装 Git: $(git --version)"
    return 0
  fi

  case "$OS" in
    ubuntu|debian)
      apt-get update -qq
      apt-get install -y -qq git
      ;;
    centos|rhel|fedora|rocky|almalinux|alinux)
      yum install -y git 2>/dev/null || dnf install -y git
      ;;
    *)
      log_info "请手动安装 Git: https://git-scm.com/"
      return 1
      ;;
  esac

  log_info "Git 安装完成: $(git --version)"
}

# ----- 安装 Node.js 24 + pnpm -----
install_node() {
  log_step "安装 Node.js 24 + pnpm"

  if command -v node >/dev/null 2>&1; then
    local nver
    nver=$(node --version)
    log_info "已安装 Node.js: $nver"
  else
    case "$OS" in
      ubuntu|debian)
        curl -fsSL https://deb.nodesource.com/setup_24.x | bash -
        apt-get install -y -qq nodejs
        ;;
      centos|rhel|fedora|rocky|almalinux|alinux)
        curl -fsSL https://rpm.nodesource.com/setup_24.x | bash -
        yum install -y nodejs 2>/dev/null || dnf install -y nodejs
        ;;
      *)
        log_info "请手动安装 Node.js 24: https://nodejs.org/"
        return 1
        ;;
    esac
    log_info "Node.js 安装完成: $(node --version)"
  fi

  # 安装 pnpm
  if command -v pnpm >/dev/null 2>&1; then
    log_info "已安装 pnpm: $(pnpm --version)"
  else
    corepack enable
    corepack prepare pnpm@latest --activate
    log_info "pnpm 安装完成: $(pnpm --version)"
  fi
}

# ----- 配置 Maven 镜像（阿里云 + 华为云）-----
configure_maven_mirrors() {
  log_step "配置 Maven 国内镜像"

  local m2_dir="${HOME}/.m2"
  local settings_file="${m2_dir}/settings.xml"
  local src_settings="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/maven/settings.xml"

  mkdir -p "$m2_dir"

  if [ -f "$src_settings" ]; then
    cp "$src_settings" "$settings_file"
    log_info "Maven settings.xml → ${settings_file}"
  else
    # 兜底：直接写入
    cat > "$settings_file" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
    <mirrors>
        <mirror>
            <id>aliyunmaven</id>
            <mirrorOf>central</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
        <mirror>
            <id>huaweicloud</id>
            <mirrorOf>central</mirrorOf>
            <name>华为云 Maven 镜像</name>
            <url>https://repo.huaweicloud.com/repository/maven</url>
        </mirror>
    </mirrors>
</settings>
XML
    log_info "Maven settings.xml 已写入（阿里云 + 华为云）"
  fi
}

# ----- 帮助 -----
show_help() {
  cat <<EOF
Snail AI CI/CD 构建环境安装脚本

用法: bash script/ci-setup.sh [命令]

命令:
  (无参数)       安装全部：JDK 21 + Maven + Git + Node.js 24 + pnpm + Maven 镜像
  jdk            仅安装 JDK 21 (BellSoft Liberica)
  maven          仅安装 Maven + Maven 国内镜像
  git            仅安装 Git
  node           仅安装 Node.js 24 + pnpm
  maven-mirror   仅配置 Maven 国内镜像（阿里云 + 华为云）
  --help         显示此帮助

支持发行版: Ubuntu / Debian / CentOS / RHEL / Fedora / Rocky / AlmaLinux
EOF
  exit 0
}

# ============================================================
# 入口
# ============================================================
# 需要 root 权限
if [ "$(id -u)" -ne 0 ]; then
  echo "请使用 root 权限运行: sudo bash script/ci-setup.sh"
  exit 1
fi

detect_os
log_info "检测到系统: ${OS} ${OS_VERSION:-}"

case "${1:-}" in
  --help|-h) show_help ;;
  jdk)           install_jdk ;;
  maven)         install_maven && configure_maven_mirrors ;;
  git)           install_git ;;
  node)          install_node ;;
  maven-mirror)  configure_maven_mirrors ;;
  "")
    install_jdk
    install_maven
    install_git
    install_node
    configure_maven_mirrors
    log_step "全部安装完成！"
    echo ""
    java -version 2>&1 || true
    mvn --version 2>&1 | head -1 || true
    git --version || true
    node --version || true
    pnpm --version || true
    ;;
  *)
    echo "未知命令: $1"
    show_help
    ;;
esac
