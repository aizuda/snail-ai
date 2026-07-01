@echo off
chcp 65001 >nul
set HTTP_PROXY=
set HTTPS_PROXY=
set http_proxy=
set https_proxy=
set NO_PROXY=localhost,127.0.0.1
cd /d %~dp0server
pip install -r requirements.txt
python -m uvicorn main:app --host 0.0.0.0 --port 5100
