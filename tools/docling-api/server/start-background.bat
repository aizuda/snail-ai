@echo off
chcp 65001 >nul
title Docling 后台服务

echo.
echo ========================================
echo   Docling API — 后台启动
echo ========================================
echo.

REM 用 pythonw 启动，没有控制台窗口
start "" /B pythonw -m uvicorn main:app --host 0.0.0.0 --port 5100

echo 服务已后台启动: http://localhost:5100
echo API 文档:       http://localhost:5100/docs
echo.
echo 查看状态: curl http://localhost:5100/health
echo 停止服务: taskkill /f /im pythonw.exe   （注意：会杀掉所有 pythonw 进程）
echo.
pause
