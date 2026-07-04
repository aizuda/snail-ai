@echo off
chcp 65001 >nul
title PaddleOCR API GPU Background

cd /d %~dp0

if "%PADDLE_OCR_HOST%"=="" set PADDLE_OCR_HOST=0.0.0.0
if "%PADDLE_OCR_PORT%"=="" set PADDLE_OCR_PORT=8866
if "%PADDLE_OCR_DEVICE%"=="" set PADDLE_OCR_DEVICE=gpu:0
if "%PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE%"=="" set PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE=8
if "%PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE%"=="" set PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE=8

echo.
echo ========================================
echo   PaddleOCR API - GPU Background Start
echo ========================================
echo.

if exist ".venv\Scripts\python.exe" (
    start "PaddleOCR GPU API" /MIN ".venv\Scripts\python.exe" -m uvicorn main:app --host %PADDLE_OCR_HOST% --port %PADDLE_OCR_PORT%
) else (
    start "PaddleOCR GPU API" /MIN python -m uvicorn main:app --host %PADDLE_OCR_HOST% --port %PADDLE_OCR_PORT%
)

echo Service started: http://127.0.0.1:%PADDLE_OCR_PORT%
echo Device:          %PADDLE_OCR_DEVICE%
echo Docs:            http://127.0.0.1:%PADDLE_OCR_PORT%/docs
echo.
echo Health: curl http://127.0.0.1:%PADDLE_OCR_PORT%/health
echo Stop:   taskkill /f /im python.exe
echo.
pause
