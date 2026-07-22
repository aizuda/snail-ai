@echo off
chcp 65001 >nul
set HTTP_PROXY=
set HTTPS_PROXY=
set http_proxy=
set https_proxy=
set NO_PROXY=localhost,127.0.0.1
set "DOCLING_MODEL_DIR=%USERPROFILE%\.cache\docling\models"

cd /d "%~dp0server"
python -m uvicorn main:app --host 0.0.0.0 --port 5100
