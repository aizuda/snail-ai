"""
Docling REST API — 文档转换服务
启动: uvicorn main:app --host 0.0.0.0 --port 5100
  或: python main.py

端点:
  GET  /health                    健康检查
  POST /convert                   同步转换（一步返回）
  POST /v1/convert/file/async     异步提交任务
  GET  /v1/status/poll/{task_id}  轮询任务状态
  GET  /v1/result/{task_id}       获取解析结果
"""

import os
import sys
import uuid
import time
import threading
import tempfile
import shutil
import logging
from pathlib import Path
from typing import Optional, Dict

from fastapi import FastAPI, File, Form, UploadFile, HTTPException, Request
from fastapi.responses import PlainTextResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from docling.document_converter import DocumentConverter, PdfFormatOption
from docling.datamodel.base_models import InputFormat
from docling.datamodel.pipeline_options import PdfPipelineOptions, RapidOcrOptions
from docling.exceptions import ConversionError
from docling_core.types.doc.base import ImageRefMode

# ---- 日志 ----
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("docling-api")

# ---- 配置 ----
API_KEY: Optional[str] = os.environ.get("DOCLING_API_KEY")
DEFAULT_HOST = os.environ.get("DOCLING_HOST", "0.0.0.0")
DEFAULT_PORT = int(os.environ.get("DOCLING_PORT", "5100"))
TASK_TTL_SECONDS = int(os.environ.get("DOCLING_TASK_TTL", "3600"))
MAX_CONCURRENT_CONVERTS = int(os.environ.get("DOCLING_MAX_CONCURRENCY", "3"))
DOCLING_MODEL_DIR = Path(os.environ.get("DOCLING_MODEL_DIR", str(Path.home() / ".cache" / "docling" / "models")))
DOCLING_OCR_BACKEND = "onnxruntime"
DOCLING_OCR_LANG = ["chinese"]
DEFAULT_IMAGE_EXPORT_MODE = "embedded"

app = FastAPI(title="Docling API", version="2.1")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_converter: Optional[DocumentConverter] = None
_converter_warm = False
_convert_semaphore = threading.Semaphore(MAX_CONCURRENT_CONVERTS)

# ---- 异步任务存储 ----
_tasks: Dict[str, dict] = {}
_tasks_lock = threading.Lock()


def _resolve_image_ref_mode(image_export_mode: Optional[str]) -> ImageRefMode:
    normalized = (image_export_mode or DEFAULT_IMAGE_EXPORT_MODE).strip().lower()
    if normalized == ImageRefMode.PLACEHOLDER.value:
        return ImageRefMode.PLACEHOLDER
    if normalized == ImageRefMode.REFERENCED.value:
        return ImageRefMode.REFERENCED
    return ImageRefMode.EMBEDDED


def _build_pipeline_options(
    do_ocr: bool,
    do_table_structure: bool,
    image_export_mode: Optional[str] = DEFAULT_IMAGE_EXPORT_MODE,
) -> PdfPipelineOptions:
    opts = PdfPipelineOptions()
    opts.do_ocr = do_ocr
    opts.do_table_structure = do_table_structure
    opts.artifacts_path = DOCLING_MODEL_DIR
    opts.generate_picture_images = _resolve_image_ref_mode(image_export_mode) != ImageRefMode.PLACEHOLDER
    if do_ocr:
        opts.ocr_options = RapidOcrOptions(lang=DOCLING_OCR_LANG, backend=DOCLING_OCR_BACKEND)
    return opts


def _get_converter(
    do_ocr: bool = True,
    do_table_structure: bool = True,
    image_export_mode: str = DEFAULT_IMAGE_EXPORT_MODE,
) -> DocumentConverter:
    """获取或创建 DocumentConverter，OCR/表格开关按参数配置。

    首次初始化后 _converter 会被缓存复用，因为实际转换时 pipeline_options
    会通过 format_options 重新传入，不需要重新创建 converter 实例。
    """
    global _converter, _converter_warm
    if _converter is None:
        logger.info("正在初始化 DocumentConverter（首次会下载模型，请耐心等待）...")
        pdf_opts = _build_pipeline_options(do_ocr, do_table_structure, image_export_mode)
        format_options = {InputFormat.PDF: PdfFormatOption(pipeline_options=pdf_opts)}
        _converter = DocumentConverter(format_options=format_options)
        _converter_warm = True
        logger.info("DocumentConverter 初始化完成")
    return _converter


def _cleanup_expired_tasks():
    now = time.time()
    with _tasks_lock:
        expired = [tid for tid, t in _tasks.items() if now - t["created_at"] > TASK_TTL_SECONDS]
        for tid in expired:
            task = _tasks.pop(tid, None)
            if task and task.get("tmp_dir") and Path(task["tmp_dir"]).exists():
                shutil.rmtree(task["tmp_dir"], ignore_errors=True)


def _do_convert(
    source,
    to_formats: list,
    do_ocr: bool,
    do_table_structure: bool,
    image_export_mode: str = DEFAULT_IMAGE_EXPORT_MODE,
) -> dict:
    """执行一次转换，返回包含 json_content / md_content 的 dict。"""
    image_mode = _resolve_image_ref_mode(image_export_mode)
    pdf_opts = _build_pipeline_options(do_ocr, do_table_structure, image_export_mode)
    format_options = {InputFormat.PDF: PdfFormatOption(pipeline_options=pdf_opts)}
    converter = DocumentConverter(format_options=format_options)

    result = converter.convert(source)
    doc = result.document

    json_content = doc.export_to_dict()
    md_content = doc.export_to_markdown(image_mode=image_mode) if "md" in to_formats else ""

    return {
        "document": {
            "json_content": json_content,
            "md_content": md_content,
        }
    }


def _run_async_convert(task_id: str, source, tmp_dir: Optional[str], to_formats: list,
                       do_ocr: bool, do_table_structure: bool,
                       image_export_mode: str = DEFAULT_IMAGE_EXPORT_MODE):
    try:
        _convert_semaphore.acquire()
        logger.info(f"[{task_id}] 开始转换 (ocr={do_ocr}, tables={do_table_structure})")
        result = _do_convert(source, to_formats, do_ocr, do_table_structure, image_export_mode)
        doc_pages = result["document"]["json_content"].get("pages", {}) if isinstance(result["document"]["json_content"], dict) else {}
        logger.info(f"[{task_id}] 转换成功, pages={len(doc_pages) if doc_pages else '?'}")

        with _tasks_lock:
            _tasks[task_id]["task_status"] = "success"
            _tasks[task_id]["result"] = result

    except Exception as e:
        logger.error(f"[{task_id}] 转换失败: {e}")
        with _tasks_lock:
            _tasks[task_id]["task_status"] = "failure"
            _tasks[task_id]["error"] = str(e)
    finally:
        _convert_semaphore.release()
        if tmp_dir and Path(tmp_dir).exists():
            shutil.rmtree(tmp_dir, ignore_errors=True)


# ---- 认证中间件 ----
@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    if API_KEY:
        if request.url.path not in ("/health", "/docs", "/openapi.json", "/redoc"):
            key = request.headers.get("X-API-Key", "")
            if key != API_KEY:
                return PlainTextResponse(
                    content='{"detail":"无效的 API Key"}',
                    status_code=401,
                    media_type="application/json",
                )
    return await call_next(request)


# ============================================================
#  同步端点（保持兼容）
# ============================================================

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "version": "2.1",
        "model_loaded": _converter_warm,
    }


@app.post("/convert")
async def convert(
    file: Optional[UploadFile] = File(None),
    url: Optional[str] = Form(None),
    to: str = Form("md"),
    ocr: bool = Form(True),
    tables: bool = Form(True),
):
    """同步转换，一步返回。参数名保持 ocr/tables 向后兼容。"""
    if not file and not url:
        raise HTTPException(status_code=400, detail="提供 `file` 或 `url`")

    tmp_dir = None
    source = None
    acquired = _convert_semaphore.acquire(blocking=False)
    if not acquired:
        raise HTTPException(status_code=503, detail="服务繁忙，请稍后重试")

    try:
        if file:
            suffix = Path(file.filename).suffix if file.filename else ".tmp"
            tmp_dir = Path(tempfile.mkdtemp(prefix="docling_"))
            tmp_path = tmp_dir / f"input{suffix}"
            with tmp_path.open("wb") as f:
                shutil.copyfileobj(file.file, f)
            source = tmp_path
            logger.info(f"转换文件: {file.filename} ({tmp_path.stat().st_size} bytes)")
        else:
            source = url
            logger.info(f"转换 URL: {url}")

        convert_result = _do_convert(source, [to], ocr, tables)
        doc = convert_result["document"]

        if to == "json":
            return doc["json_content"]
        elif to == "html":
            html = doc["json_content"].get("html", "") if isinstance(doc["json_content"], dict) else ""
            return PlainTextResponse(content=html or doc["md_content"], media_type="text/html")
        elif to == "text":
            from docling_core.types.doc.base import DoclingDocument
            text_result = _get_converter(ocr, tables).convert(source)
            text = text_result.document.export_to_text()
            return PlainTextResponse(content=text, media_type="text/plain")
        else:
            return PlainTextResponse(content=doc["md_content"], media_type="text/markdown")

    except ConversionError as e:
        logger.error(f"转换失败: {e}")
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        logger.error(f"未知错误: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        _convert_semaphore.release()
        if tmp_dir and tmp_dir.exists():
            shutil.rmtree(tmp_dir, ignore_errors=True)


# ============================================================
#  异步三步接口（Snail AI 对接用）
# ============================================================

@app.post("/v1/convert/file/async")
async def convert_async(
    files: list[UploadFile] = File(...),
    to_formats: list[str] = Form(default=["md", "json"]),
    do_ocr: bool = Form(True),
    do_table_structure: bool = Form(True),
    image_export_mode: str = Form("embedded"),
    pdf_backend: Optional[str] = Form(None),
    ocr_lang: Optional[list[str]] = Form(default=None),
):
    """异步提交文档转换任务，返回 task_id。

    参数名对齐 Snail AI DoclingClient.applyOptions() 的字段名。
    image_export_mode 会映射到 Docling 图片导出模式；pdf_backend / ocr_lang 当前版本保留但暂未映射。
    """
    _cleanup_expired_tasks()

    if not files:
        raise HTTPException(status_code=400, detail="至少上传一个文件")

    file = files[0]
    suffix = Path(file.filename).suffix if file.filename else ".tmp"
    tmp_dir = Path(tempfile.mkdtemp(prefix="docling_async_"))
    tmp_path = tmp_dir / f"input{suffix}"
    with tmp_path.open("wb") as f:
        shutil.copyfileobj(file.file, f)

    task_id = uuid.uuid4().hex
    logger.info(f"[{task_id}] 异步任务已创建: {file.filename} ({tmp_path.stat().st_size} bytes)")

    with _tasks_lock:
        _tasks[task_id] = {
            "task_id": task_id,
            "task_status": "pending",
            "created_at": time.time(),
            "filename": file.filename,
            "tmp_dir": str(tmp_dir),
        }

    thread = threading.Thread(
        target=_run_async_convert,
        args=(task_id, tmp_path, str(tmp_dir), to_formats, do_ocr, do_table_structure, image_export_mode),
        daemon=True,
    )
    thread.start()

    return {"task_id": task_id}


@app.get("/v1/status/poll/{task_id}")
async def poll_status(task_id: str):
    with _tasks_lock:
        task = _tasks.get(task_id)

    if task is None:
        return {"task_status": "failure", "error": "task not found"}

    return {"task_status": task.get("task_status", "pending")}


@app.get("/v1/result/{task_id}")
async def get_result(task_id: str):
    with _tasks_lock:
        task = _tasks.get(task_id)

    if task is None:
        raise HTTPException(status_code=404, detail="任务不存在或已过期")

    if task.get("task_status") != "success":
        raise HTTPException(status_code=400, detail=f"任务未完成，当前状态: {task.get('task_status')}")

    result = task.get("result")
    if result is None:
        raise HTTPException(status_code=500, detail="结果数据缺失")

    return JSONResponse(content=result)


# ---- 启动入口 ----
if __name__ == "__main__":
    import uvicorn

    host = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_HOST
    port = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_PORT

    logger.info("=" * 50)
    logger.info(f" Docling API v2.1")
    logger.info(f" 地址: http://{host}:{port}")
    logger.info(f" 文档: http://{host}:{port}/docs")
    logger.info(f" 认证: {'启用 (X-API-Key)' if API_KEY else '关闭'}")
    logger.info(f" 最大并发: {MAX_CONCURRENT_CONVERTS}")
    logger.info(f" 异步 TTL: {TASK_TTL_SECONDS}s")
    logger.info("=" * 50)

    uvicorn.run(app, host=host, port=port, log_level="info")
