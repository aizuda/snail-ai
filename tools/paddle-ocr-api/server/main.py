"""
PaddleOCR REST API.

Start:
  python -m uvicorn main:app --host 0.0.0.0 --port 8866

Endpoints:
  GET  /health
  POST /ocr
  POST /ocr/batch
"""

from __future__ import annotations

import base64
import binascii
import logging
import os
import sys
import tempfile
import threading
import time
from pathlib import Path
from typing import Any, Optional

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

try:
    from paddleocr import PaddleOCR
except Exception as exc:  # pragma: no cover - startup dependency guard
    PaddleOCR = None
    PADDLE_IMPORT_ERROR = exc
else:
    PADDLE_IMPORT_ERROR = None

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("paddle-ocr-api")


def _env_bool(name: str, default: bool) -> bool:
    value = os.environ.get(name)
    if value is None or not value.strip():
        return default
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    logger.warning("Invalid boolean env %s=%s, fallback to %s", name, value, default)
    return default


def _env_int_optional(name: str) -> Optional[int]:
    value = os.environ.get(name)
    if value is None or not value.strip():
        return None
    try:
        parsed = int(value)
    except ValueError:
        logger.warning("Invalid integer env %s=%s, ignored", name, value)
        return None
    if parsed <= 0:
        logger.warning("Invalid positive integer env %s=%s, ignored", name, value)
        return None
    return parsed


API_KEY: Optional[str] = os.environ.get("PADDLE_OCR_API_KEY")
DEFAULT_HOST = os.environ.get("PADDLE_OCR_HOST", "0.0.0.0")
DEFAULT_PORT = int(os.environ.get("PADDLE_OCR_PORT", "8866"))
OCR_LANG = os.environ.get("PADDLE_OCR_LANG", "ch")
OCR_DEVICE = os.environ.get("PADDLE_OCR_DEVICE") or None
OCR_CPU_THREADS = _env_int_optional("PADDLE_OCR_CPU_THREADS")
OCR_USE_TEXTLINE_ORIENTATION = _env_bool("PADDLE_OCR_USE_TEXTLINE_ORIENTATION", True)
OCR_TEXT_RECOGNITION_BATCH_SIZE = _env_int_optional("PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE")
OCR_TEXTLINE_ORIENTATION_BATCH_SIZE = _env_int_optional("PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE")
MAX_IMAGE_BYTES = int(os.environ.get("PADDLE_OCR_MAX_IMAGE_MB", "20")) * 1024 * 1024
MAX_CONCURRENT_OCR = int(os.environ.get("PADDLE_OCR_MAX_CONCURRENCY", "1"))
OCR_QUEUE_TIMEOUT_SECONDS = int(os.environ.get("PADDLE_OCR_QUEUE_TIMEOUT_SECONDS", "300"))

app = FastAPI(title="PaddleOCR API", version="1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_ocr_model: Any = None
_ocr_lock = threading.Lock()
_ocr_semaphore = threading.Semaphore(MAX_CONCURRENT_OCR)


class OcrRequest(BaseModel):
    imageBase64: str = Field(..., min_length=1)
    fileName: Optional[str] = None


class OcrBatchImage(OcrRequest):
    id: str = Field(..., min_length=1)


class OcrBatchRequest(BaseModel):
    images: list[OcrBatchImage] = Field(..., min_length=1)


@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    if API_KEY and request.url.path not in ("/health", "/docs", "/openapi.json", "/redoc"):
        key = request.headers.get("X-API-Key", "")
        if key != API_KEY:
            return JSONResponse(content={"detail": "Invalid API key"}, status_code=401)
    return await call_next(request)


@app.get("/health")
async def health():
    return {
        "status": "ok" if PADDLE_IMPORT_ERROR is None else "dependency_error",
        "version": "1.0",
        "model_loaded": _ocr_model is not None,
        "lang": OCR_LANG,
        "maxConcurrency": MAX_CONCURRENT_OCR,
        "queueTimeoutSeconds": OCR_QUEUE_TIMEOUT_SECONDS,
        "device": OCR_DEVICE,
        "cpuThreads": OCR_CPU_THREADS,
        "useTextlineOrientation": OCR_USE_TEXTLINE_ORIENTATION,
        "textRecognitionBatchSize": OCR_TEXT_RECOGNITION_BATCH_SIZE,
        "textlineOrientationBatchSize": OCR_TEXTLINE_ORIENTATION_BATCH_SIZE,
        "error": str(PADDLE_IMPORT_ERROR) if PADDLE_IMPORT_ERROR else None,
    }


@app.post("/ocr")
def recognize(request: OcrRequest):
    try:
        result = _recognize_with_queue(request)
        return {
            "text": result["text"],
            "results": result["results"],
            "elapsedMs": result["elapsedMs"],
        }
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("OCR failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/ocr/batch")
def recognize_batch(request: OcrBatchRequest):
    items = _recognize_batch_with_queue(request.images)
    return {
        "status": _resolve_batch_status(items),
        "items": items,
    }


def _recognize_batch_with_queue(images: list[OcrBatchImage]) -> list[dict[str, Any]]:
    if not _ocr_semaphore.acquire(timeout=OCR_QUEUE_TIMEOUT_SECONDS):
        error = HTTPException(status_code=503, detail="OCR service is busy")
        return [_batch_item_failed(image, error) for image in images]
    try:
        return _recognize_images(images)
    finally:
        _ocr_semaphore.release()


def _batch_item_from_result(image: OcrBatchImage, result: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": image.id,
        "status": result["status"],
        "text": result["text"],
        "results": result["results"],
        "elapsedMs": result["elapsedMs"],
        "error": "",
    }


def _batch_item_failed(image: OcrBatchImage, exc: Exception) -> dict[str, Any]:
    return {
        "id": image.id,
        "status": "FAILED",
        "text": "",
        "results": [],
        "elapsedMs": 0,
        "error": _exception_message(exc),
    }


def _recognize_with_queue(request: OcrRequest) -> dict[str, Any]:
    if not _ocr_semaphore.acquire(timeout=OCR_QUEUE_TIMEOUT_SECONDS):
        raise HTTPException(status_code=503, detail="OCR service is busy")
    try:
        return _recognize_image(request)
    finally:
        _ocr_semaphore.release()


def _recognize_image(request: OcrRequest) -> dict[str, Any]:
    start_time = time.perf_counter()
    image_path = _write_temp_image(request)
    try:
        model = _get_ocr_model()
        raw_result = _predict(model, image_path)
        elapsed_ms = int((time.perf_counter() - start_time) * 1000)
        return _recognition_result(raw_result, elapsed_ms)
    finally:
        Path(image_path).unlink(missing_ok=True)


def _recognize_images(images: list[OcrBatchImage]) -> list[dict[str, Any]]:
    start_time = time.perf_counter()
    items: list[Optional[dict[str, Any]]] = [None] * len(images)
    prepared_images: list[dict[str, Any]] = []
    image_paths: list[str] = []

    for index, image in enumerate(images):
        try:
            image_path = _write_temp_image(image)
            image_paths.append(image_path)
            prepared_images.append({"index": index, "image": image, "path": image_path})
        except Exception as exc:
            logger.warning("Batch OCR item failed. id=%s fileName=%s error=%s", image.id, image.fileName, exc)
            items[index] = _batch_item_failed(image, exc)

    try:
        if prepared_images:
            model = _get_ocr_model()
            raw_result = _predict(model, [item["path"] for item in prepared_images])
            raw_items = _split_batch_raw_result(raw_result, len(prepared_images))
            elapsed_ms = int((time.perf_counter() - start_time) * 1000)
            for prepared, raw_item in zip(prepared_images, raw_items):
                result = _recognition_result(raw_item, elapsed_ms)
                items[prepared["index"]] = _batch_item_from_result(prepared["image"], result)
    except Exception as exc:
        logger.warning("Batch OCR prediction failed. count=%s error=%s", len(prepared_images), exc)
        for prepared in prepared_images:
            items[prepared["index"]] = _batch_item_failed(prepared["image"], exc)
    finally:
        for image_path in image_paths:
            Path(image_path).unlink(missing_ok=True)

    return [item if item is not None else _batch_item_failed(images[index], RuntimeError("OCR item missing"))
            for index, item in enumerate(items)]


def _write_temp_image(request: OcrRequest) -> str:
    image_bytes = _decode_image_base64(request.imageBase64)
    suffix = _resolve_suffix(request.fileName)

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(image_bytes)
        return tmp.name


def _recognition_result(raw_result: Any, elapsed_ms: int) -> dict[str, Any]:
    texts = _extract_texts(raw_result)
    text = "\n".join(_deduplicate(texts))
    return {
        "status": "SUCCESS" if text else "EMPTY",
        "text": text,
        "results": texts,
        "elapsedMs": elapsed_ms,
    }


def _split_batch_raw_result(raw_result: Any, count: int) -> list[Any]:
    if count == 1:
        if isinstance(raw_result, (list, tuple)) and len(raw_result) == 1:
            return [raw_result[0]]
        return [raw_result]
    if not isinstance(raw_result, (list, tuple)):
        raise RuntimeError("Batch OCR result is not a list")
    if len(raw_result) != count:
        raise RuntimeError(f"Batch OCR result count mismatch: expected {count}, got {len(raw_result)}")
    return list(raw_result)


def _resolve_batch_status(items: list[dict[str, Any]]) -> str:
    if all(item.get("status") == "FAILED" for item in items):
        return "FAILED"
    return "SUCCESS"


def _exception_message(exc: Exception) -> str:
    if isinstance(exc, HTTPException):
        return str(exc.detail)
    return str(exc)


def _decode_image_base64(value: str) -> bytes:
    payload = value.strip()
    if "," in payload and payload.lower().startswith("data:"):
        payload = payload.split(",", 1)[1]
    try:
        image_bytes = base64.b64decode(payload, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise HTTPException(status_code=400, detail="Invalid imageBase64") from exc

    if not image_bytes:
        raise HTTPException(status_code=400, detail="Empty image")
    if len(image_bytes) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image is too large")
    return image_bytes


def _resolve_suffix(file_name: Optional[str]) -> str:
    if not file_name:
        return ".png"
    suffix = Path(file_name).suffix.lower()
    if suffix in {".png", ".jpg", ".jpeg", ".bmp", ".webp", ".tif", ".tiff"}:
        return suffix
    return ".png"


def _get_ocr_model():
    global _ocr_model
    if PADDLE_IMPORT_ERROR is not None:
        raise HTTPException(status_code=500, detail=f"PaddleOCR import failed: {PADDLE_IMPORT_ERROR}")
    if _ocr_model is not None:
        return _ocr_model

    with _ocr_lock:
        if _ocr_model is not None:
            return _ocr_model
        logger.info("Loading PaddleOCR model, lang=%s", OCR_LANG)
        _ocr_model = _create_ocr_model()
        logger.info("PaddleOCR model loaded")
        return _ocr_model


def _create_ocr_model():
    try:
        return PaddleOCR(**_ocr_model_options())
    except TypeError:
        return PaddleOCR(use_angle_cls=OCR_USE_TEXTLINE_ORIENTATION, lang=OCR_LANG)


def _ocr_model_options() -> dict[str, Any]:
    options: dict[str, Any] = {
        "lang": OCR_LANG,
        "use_doc_orientation_classify": False,
        "use_doc_unwarping": False,
        "use_textline_orientation": OCR_USE_TEXTLINE_ORIENTATION,
    }
    optional_options = {
        "device": OCR_DEVICE,
        "cpu_threads": OCR_CPU_THREADS,
        "text_recognition_batch_size": OCR_TEXT_RECOGNITION_BATCH_SIZE,
        "textline_orientation_batch_size": OCR_TEXTLINE_ORIENTATION_BATCH_SIZE,
    }
    for key, value in optional_options.items():
        if value is not None:
            options[key] = value
    return options


def _predict(model, image_input: str | list[str]):
    if hasattr(model, "predict"):
        return model.predict(image_input)
    if isinstance(image_input, list):
        return [model.ocr(image_path, cls=OCR_USE_TEXTLINE_ORIENTATION) for image_path in image_input]
    return model.ocr(image_input, cls=OCR_USE_TEXTLINE_ORIENTATION)


def _extract_texts(node: Any) -> list[str]:
    values: list[str] = []
    _collect_texts(_normalize_node(node), values)
    return [item.strip() for item in values if item and item.strip()]


def _normalize_node(node: Any) -> Any:
    if hasattr(node, "json"):
        json_value = getattr(node, "json")
        if not callable(json_value):
            return json_value
    if hasattr(node, "to_json"):
        try:
            return node.to_json()
        except Exception:
            pass
    if hasattr(node, "__dict__") and not isinstance(node, type):
        return vars(node)
    return node


def _collect_texts(node: Any, values: list[str]) -> None:
    node = _normalize_node(node)
    if node is None:
        return
    if isinstance(node, dict):
        has_ocr_text = False
        for key in ("text", "ocrText", "content", "rec_text", "rec_texts"):
            if key in node:
                _collect_texts(node[key], values)
                has_ocr_text = True
        if has_ocr_text:
            return

        for key in ("data", "res", "result", "results", "ocr_result"):
            if key in node:
                _collect_texts(node[key], values)
        return
    if isinstance(node, (list, tuple)):
        if _looks_like_text_score_pair(node):
            values.append(str(node[0]))
            return
        for item in node:
            _collect_texts(item, values)
        return
    if isinstance(node, str):
        values.append(node)


def _looks_like_text_score_pair(node: list[Any] | tuple[Any, ...]) -> bool:
    return len(node) == 2 and isinstance(node[0], str) and isinstance(node[1], (int, float))


def _deduplicate(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        text = value.strip()
        if not text or text in seen:
            continue
        seen.add(text)
        result.append(text)
    return result


if __name__ == "__main__":
    import uvicorn

    host = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_HOST
    port = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_PORT
    logger.info("PaddleOCR API v1.0")
    logger.info("Address: http://%s:%s", host, port)
    logger.info("Docs: http://%s:%s/docs", host, port)
    logger.info("Auth: %s", "enabled" if API_KEY else "disabled")
    uvicorn.run(app, host=host, port=port, log_level="info")
