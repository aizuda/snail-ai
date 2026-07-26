@echo off
chcp 65001 >nul
title PaddleOCR API Background

cd /d %~dp0

if "%PADDLE_OCR_HOST%"=="" set PADDLE_OCR_HOST=0.0.0.0
if "%PADDLE_OCR_PORT%"=="" set PADDLE_OCR_PORT=8866

echo.
echo ========================================
echo   PaddleOCR API - Background Start
echo ========================================
echo.

if exist ".venv\Scripts\python.exe" (
    start "PaddleOCR API" /MIN ".venv\Scripts\python.exe" -m uvicorn main:app --host %PADDLE_OCR_HOST% --port %PADDLE_OCR_PORT%
) else (
    start "PaddleOCR API" /MIN python -m uvicorn main:app --host %PADDLE_OCR_HOST% --port %PADDLE_OCR_PORT%
)

echo Service started: http://127.0.0.1:%PADDLE_OCR_PORT%
echo Docs:            http://127.0.0.1:%PADDLE_OCR_PORT%/docs
echo.
echo Health: curl http://127.0.0.1:%PADDLE_OCR_PORT%/health
echo Stop:   taskkill /f /im python.exe
echo.
pause
