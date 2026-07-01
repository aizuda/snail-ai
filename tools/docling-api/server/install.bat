@echo off
chcp 65001 >nul
title Docling 一键安装

echo.
echo ========================================
echo   Docling API — 一键安装脚本
echo ========================================
echo.

REM 1. 检查 Python
echo [1/4] 检查 Python 环境 ...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Python，请先安装 Python 3.9+
    echo       下载地址: https://www.python.org/downloads/
    echo       安装时请勾选 "Add Python to PATH"
    pause
    exit /b 1
)
python --version
echo.

REM 2. 安装依赖
echo [2/4] 安装 Python 依赖 ...
pip install -r "%~dp0requirements.txt" -q
if %errorlevel% neq 0 (
    echo [错误] pip 安装失败，请检查网络连接
    pause
    exit /b 1
)
echo 依赖安装完成
echo.

REM 3. 预下载模型
echo [3/4] 预下载 docling 模型（首次约 2GB，请耐心等待）...
echo.
python -c "from docling.document_converter import DocumentConverter; print('模型就绪')" 2>&1
echo.

REM 4. 是否注册为 Windows 服务
echo [4/4] 注册 Windows 服务？
echo.
set /p REGISTER="是否注册为 Windows 服务（开机自启）？ [Y/n]: "
if /i "%REGISTER%"=="" set REGISTER=Y
if /i "%REGISTER%"=="Y" (
    echo 请以管理员身份运行: "%~dp0install-service.bat"
) else (
    echo 跳过服务注册。可随时双击 "start-background.bat" 启动
)

echo.
echo ========================================
echo   安装完成！
echo ========================================
echo.
echo 启动方式:
echo   - 后台启动: 双击 server\start-background.bat
echo   - 注册服务: 以管理员身份运行 server\install-service.bat
echo.
echo 地址:     http://localhost:5100
echo API 文档: http://localhost:5100/docs
echo 对接文档: server\API.md
echo.
pause
