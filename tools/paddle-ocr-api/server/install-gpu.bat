@echo off
chcp 65001 >nul
title PaddleOCR API GPU Install

echo.
echo ========================================
echo   PaddleOCR API - GPU Install
echo ========================================
echo.

cd /d %~dp0

echo [1/6] Checking NVIDIA GPU ...
nvidia-smi
if %errorlevel% neq 0 (
    echo [ERROR] NVIDIA GPU or driver was not detected.
    pause
    exit /b 1
)
echo.

echo [2/6] Checking Python ...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python was not found. Please install Python 3.10+ and enable "Add Python to PATH".
    pause
    exit /b 1
)
python --version
echo.

echo [3/6] Creating virtual environment ...
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

echo [4/6] Upgrading pip ...
".venv\Scripts\python.exe" -m pip install -U pip
if %errorlevel% neq 0 (
    echo [ERROR] Failed to upgrade pip.
    pause
    exit /b 1
)
echo.

echo [5/6] Installing PaddlePaddle GPU runtime ...
set PADDLEPADDLE_VERSION=3.2.2
".venv\Scripts\python.exe" -m pip uninstall -y paddlepaddle
".venv\Scripts\python.exe" -m pip install paddlepaddle-gpu==%PADDLEPADDLE_VERSION% -i https://www.paddlepaddle.org.cn/packages/stable/cu126/
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install PaddlePaddle %PADDLEPADDLE_VERSION% GPU runtime.
    pause
    exit /b 1
)
echo.

echo [6/6] Installing PaddleOCR API dependencies ...
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install dependencies.
    pause
    exit /b 1
)
echo.

echo Verifying GPU installation ...
".venv\Scripts\python.exe" -c "import paddle; print('PaddlePaddle', paddle.__version__); print('CUDA enabled', paddle.device.is_compiled_with_cuda()); print('Device', paddle.device.get_device()); from paddleocr import PaddleOCR; print('PaddleOCR OK')"
if %errorlevel% neq 0 (
    echo [ERROR] Verification failed.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   GPU install completed
echo ========================================
echo.
if "%PADDLE_OCR_PORT%"=="" set PADDLE_OCR_PORT=8866
echo Start:
echo   - GPU foreground: ..\start-gpu.bat
echo   - CPU foreground: ..\start.bat
echo.
echo Health: http://127.0.0.1:%PADDLE_OCR_PORT%/health
echo Docs:   http://127.0.0.1:%PADDLE_OCR_PORT%/docs
echo.
pause
