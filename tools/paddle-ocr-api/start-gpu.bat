@echo off
chcp 65001 >nul
set HTTP_PROXY=
set HTTPS_PROXY=
set http_proxy=
set https_proxy=
set NO_PROXY=localhost,127.0.0.1

cd /d %~dp0server

if "%PADDLE_OCR_HOST%"=="" set PADDLE_OCR_HOST=0.0.0.0
if "%PADDLE_OCR_PORT%"=="" set PADDLE_OCR_PORT=8866
if "%PADDLE_OCR_DEVICE%"=="" set PADDLE_OCR_DEVICE=gpu:0
if "%PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE%"=="" set PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE=8
if "%PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE%"=="" set PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE=8

if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" -m uvicorn main:app --host %PADDLE_OCR_HOST% --port %PADDLE_OCR_PORT%
) else (
    python -m uvicorn main:app --host %PADDLE_OCR_HOST% --port %PADDLE_OCR_PORT%
)
