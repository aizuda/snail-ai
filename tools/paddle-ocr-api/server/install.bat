@echo off
chcp 65001 >nul
title PaddleOCR API Install

echo.
echo ========================================
echo   PaddleOCR API - Install
echo ========================================
echo.

cd /d %~dp0

echo [1/5] Checking Python ...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python was not found. Please install Python 3.10+ and enable "Add Python to PATH".
    echo         Download: https://www.python.org/downloads/
    pause
    exit /b 1
)
python --version
echo.

echo [2/5] Creating virtual environment ...
if not exist ".venv\Scripts\python.exe" (
    python -m venv .venv
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to create virtual environment.
        pause
        exit /b 1
    )
)
echo Virtual environment: %cd%\.venv
echo.

echo [3/5] Upgrading pip ...
".venv\Scripts\python.exe" -m pip install -U pip
if %errorlevel% neq 0 (
    echo [ERROR] Failed to upgrade pip.
    pause
    exit /b 1
)
echo.

echo [4/5] Installing PaddlePaddle CPU runtime ...
set PADDLEPADDLE_VERSION=3.2.2
".venv\Scripts\python.exe" -m pip install paddlepaddle==%PADDLEPADDLE_VERSION% -i https://www.paddlepaddle.org.cn/packages/stable/cpu/
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install PaddlePaddle %PADDLEPADDLE_VERSION% CPU runtime.
    pause
    exit /b 1
)
echo.

echo [5/5] Installing PaddleOCR API dependencies ...
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install dependencies.
    pause
    exit /b 1
)
echo.

echo Verifying installation ...
".venv\Scripts\python.exe" -c "import paddle; print('PaddlePaddle', paddle.__version__); from paddleocr import PaddleOCR; print('PaddleOCR OK')"
if %errorlevel% neq 0 (
    echo [ERROR] Verification failed.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Install completed
echo ========================================
echo.
if "%PADDLE_OCR_PORT%"=="" set PADDLE_OCR_PORT=8866
echo Start:
echo   - Foreground: ..\start.bat
echo   - Background: start-background.bat
echo.
echo Health: http://127.0.0.1:%PADDLE_OCR_PORT%/health
echo Docs:   http://127.0.0.1:%PADDLE_OCR_PORT%/docs
echo.
pause
