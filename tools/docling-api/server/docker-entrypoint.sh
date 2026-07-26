#!/bin/sh
set -eu

RAPIDOCR_DET_MODEL="${DOCLING_MODEL_DIR}/RapidOcr/onnx/PP-OCRv4/det/ch_PP-OCRv4_det_infer.onnx"
RAPIDOCR_REC_MODEL="${DOCLING_MODEL_DIR}/RapidOcr/onnx/PP-OCRv4/rec/ch_PP-OCRv4_rec_infer.onnx"
RAPIDOCR_KEYS="${DOCLING_MODEL_DIR}/RapidOcr/paddle/PP-OCRv4/rec/ch_PP-OCRv4_rec_infer/ppocr_keys_v1.txt"

if [ "${DOCLING_DOWNLOAD_MODELS:-true}" = "true" ]; then
    mkdir -p "${DOCLING_MODEL_DIR}"
    if [ ! -f "${RAPIDOCR_DET_MODEL}" ] || [ ! -f "${RAPIDOCR_REC_MODEL}" ] || [ ! -f "${RAPIDOCR_KEYS}" ]; then
        python -m docling.cli.models download layout tableformer rapidocr -o "${DOCLING_MODEL_DIR}"
    else
        echo "Docling models are ready: ${DOCLING_MODEL_DIR}"
    fi
elif [ ! -f "${RAPIDOCR_DET_MODEL}" ] || [ ! -f "${RAPIDOCR_REC_MODEL}" ] || [ ! -f "${RAPIDOCR_KEYS}" ]; then
    echo "WARNING: Docling OCR models are missing under ${DOCLING_MODEL_DIR}. Set DOCLING_DOWNLOAD_MODELS=true to download them at startup."
fi

exec uvicorn main:app --host "${DOCLING_HOST:-0.0.0.0}" --port "${DOCLING_PORT:-5100}"
