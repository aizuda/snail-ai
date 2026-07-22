#!/bin/sh
set -eu

if [ "${DOCLING_DOWNLOAD_MODELS:-false}" = "true" ]; then
    mkdir -p "${DOCLING_MODEL_DIR}"
    if [ -z "$(find "${DOCLING_MODEL_DIR}" -mindepth 1 -maxdepth 1 2>/dev/null)" ]; then
        python -m docling.cli.models download layout tableformer rapidocr -o "${DOCLING_MODEL_DIR}"
    else
        echo "Docling model directory is not empty: ${DOCLING_MODEL_DIR}"
    fi
fi

exec uvicorn main:app --host "${DOCLING_HOST:-0.0.0.0}" --port "${DOCLING_PORT:-5100}"
