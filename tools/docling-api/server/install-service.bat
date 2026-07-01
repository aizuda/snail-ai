@echo off
chcp 65001 >nul
title Docling Windows 服务安装

echo.
echo ========================================
echo   Docling API — 注册 Windows 服务
echo ========================================
echo.
echo 需要管理员权限。请右键 "以管理员身份运行" 本脚本。
echo.

REM 检查管理员权限
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 请以管理员身份重新运行此脚本！
    pause
    exit /b 1
)

REM 获取当前目录
set "SERVICE_DIR=%~dp0"
set "SERVICE_DIR=%SERVICE_DIR:~0,-1%"

REM 找到 pythonw.exe
for /f "tokens=*" %%i in ('where pythonw') do set "PYTHONW=%%i"
if "%PYTHONW%"=="" (
    echo [错误] 找不到 pythonw.exe，请确认 Python 已安装
    pause
    exit /b 1
)
echo Python: %PYTHONW%

REM 下载 nssm（如果还没有）
set "NSSM=%SERVICE_DIR%\nssm.exe"
if not exist "%NSSM%" (
    echo 正在下载 nssm ...
    powershell -Command "Invoke-WebRequest -Uri 'https://nssm.cc/release/nssm-2.24.zip' -OutFile '%SERVICE_DIR%\nssm.zip'" 2>nul
    if %errorlevel% neq 0 (
        echo [警告] 下载失败，请手动从 https://nssm.cc/download 下载 nssm.exe
        echo         放到 %SERVICE_DIR% 目录下后重新运行本脚本
        pause
        exit /b 1
    )
    powershell -Command "Expand-Archive -Path '%SERVICE_DIR%\nssm.zip' -DestinationPath '%SERVICE_DIR%\nssm-tmp' -Force"
    for /r "%SERVICE_DIR%\nssm-tmp" %%f in (nssm.exe) do copy /y "%%f" "%NSSM%" >nul
    del /q "%SERVICE_DIR%\nssm.zip"
    rmdir /s /q "%SERVICE_DIR%\nssm-tmp"
    echo nssm.exe 下载完成
)

REM 先停止并删除已有服务（如果存在）
"%NSSM%" stop DoclingAPI >nul 2>&1
"%NSSM%" remove DoclingAPI confirm >nul 2>&1

REM 安装服务
echo 正在注册 Windows 服务: DoclingAPI ...
"%NSSM%" install DoclingAPI "%PYTHONW%" -m uvicorn main:app --host 0.0.0.0 --port 5100
"%NSSM%" set DoclingAPI AppDirectory "%SERVICE_DIR%"
"%NSSM%" set DoclingAPI DisplayName "Docling API 文档转换服务"
"%NSSM%" set DoclingAPI Description "IBM Docling 文档解析 REST API 服务"
"%NSSM%" set DoclingAPI Start SERVICE_AUTO_START
"%NSSM%" set DoclingAPI AppStdout "%SERVICE_DIR%\service-stdout.log"
"%NSSM%" set DoclingAPI AppStderr "%SERVICE_DIR%\service-stderr.log"
"%NSSM%" set DoclingAPI AppRotateFiles 1
"%NSSM%" set DoclingAPI AppRotateBytes 10485760

REM 启动服务
echo 正在启动服务 ...
"%NSSM%" start DoclingAPI

echo.
echo ========================================
echo   安装完成！
echo ========================================
echo.
echo 服务名:    DoclingAPI
echo 状态:      已启动，开机自动运行
echo 地址:      http://localhost:5100
echo API 文档:  http://localhost:5100/docs
echo.
echo 管理命令:
echo   停止:  nssm stop DoclingAPI
echo   启动:  nssm start DoclingAPI
echo   卸载:  nssm remove DoclingAPI confirm
echo   查看:  services.msc   （搜索 DoclingAPI）
echo.
pause
