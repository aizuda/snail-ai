@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
title Setup Docling API Environment

echo.
echo ========================================
echo   Setup Docling API Environment
echo ========================================
echo.

echo [1/3] Checking Python ...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python was not found. Please install Python 3.9+ and add it to PATH.
    echo         Download: https://www.python.org/downloads/
    pause
    exit /b 1
)
python --version
echo.

echo [2/3] Installing Python dependencies ...
cd /d "%~dp0server"
python -m pip install -r requirements.txt -q
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install Python dependencies. Check network access to PyPI.
    pause
    exit /b 1
)
echo Dependencies are ready.
echo.

echo [3/3] Downloading Docling PDF and OCR models ...
set "DOCLING_MODEL_DIR=%USERPROFILE%\.cache\docling\models"

if defined DOCLING_HF_ENDPOINT (
    set "HF_ENDPOINT=%DOCLING_HF_ENDPOINT%"
    echo Using custom HuggingFace endpoint: !HF_ENDPOINT!
    call :download_models
    if errorlevel 1 goto download_failed
) else (
    echo Using default HuggingFace endpoint.
    set "HF_ENDPOINT="
    call :download_models
    if errorlevel 1 (
        echo.
        echo Default HuggingFace download failed.
        echo Retrying with HuggingFace mirror: https://hf-mirror.com
        set "HF_ENDPOINT=https://hf-mirror.com"
        call :download_models
        if errorlevel 1 goto download_failed
    )
)
echo Models are ready: %DOCLING_MODEL_DIR%
echo.

echo ========================================
echo   Environment setup complete
echo ========================================
echo.
echo Start the API with:
echo   start-docling-api.bat
echo.
pause
exit /b 0

:download_models
python -m docling.cli.models download layout tableformer rapidocr -o "%DOCLING_MODEL_DIR%"
exit /b %errorlevel%

:download_failed
echo.
echo [ERROR] Failed to download Docling PDF models.
echo         Current endpoint: %HF_ENDPOINT%
echo         If this machine cannot access HuggingFace, copy the model cache from another machine:
echo         %USERPROFILE%\.cache\docling\models
echo         Or set DOCLING_HF_ENDPOINT to an available internal mirror and run this script again.
pause
exit /b 1
