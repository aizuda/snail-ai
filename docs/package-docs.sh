#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCS_DIR="${REPO_ROOT}/docs"
OUT_DIR="${REPO_ROOT}/dist/packages"
SKIP_INSTALL=0

usage() {
  cat <<'USAGE'
Usage: bash script/package-docs.sh [--skip-install] [--out-dir <dir>]

Options:
  --skip-install   Skip pnpm install --frozen-lockfile
  --out-dir <dir>  Output directory for the generated tar.gz package
  -h, --help       Show this help message
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-install)
      SKIP_INSTALL=1
      shift
      ;;
    --out-dir)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --out-dir" >&2
        exit 1
      fi
      OUT_DIR="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if ! command -v pnpm >/dev/null 2>&1; then
  echo "pnpm is required but was not found in PATH" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "node is required but was not found in PATH" >&2
  exit 1
fi

if ! command -v tar >/dev/null 2>&1; then
  echo "tar is required but was not found in PATH" >&2
  exit 1
fi

if [[ ! -f "${DOCS_DIR}/package.json" ]]; then
  echo "docs/package.json was not found under ${REPO_ROOT}" >&2
  exit 1
fi

DOCS_VERSION="$(cd "${DOCS_DIR}" && node -p "require('./package.json').version")"
if [[ -z "${DOCS_VERSION}" ]]; then
  DOCS_VERSION="0.0.0"
fi

if [[ "${SKIP_INSTALL}" -eq 0 ]]; then
  pnpm --dir "${DOCS_DIR}" install --frozen-lockfile
fi

pnpm --dir "${DOCS_DIR}" docs:build

if [[ ! -f "${DOCS_DIR}/dist/index.html" ]]; then
  echo "docs build did not produce docs/dist/index.html" >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"
PACKAGE_NAME="snail-ai-docs-site-${DOCS_VERSION}.tar.gz"
PACKAGE_PATH="${OUT_DIR}/${PACKAGE_NAME}"

tar -czf "${PACKAGE_PATH}" -C "${DOCS_DIR}/dist" .

echo "Docs package created: ${PACKAGE_PATH}"
