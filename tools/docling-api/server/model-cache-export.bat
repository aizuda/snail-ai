@echo off
chcp 65001 >nul
title 导出 Docling 模型缓存

echo.
echo ========================================
echo   导出 Docling 模型缓存（离线部署用）
echo ========================================
echo.

set "HF_CACHE=%USERPROFILE%\.cache\huggingface\hub"
set "OUTPUT=%~dp0docling-models.zip"

if not exist "%HF_CACHE%" (
    echo [错误] 模型缓存目录不存在: %HF_CACHE%
    echo        请先运行一次 docling 下载模型
    pause
    exit /b 1
)

echo 模型缓存: %HF_CACHE%
echo 输出文件: %OUTPUT%
echo.

REM 获取缓存大小
for /f "tokens=3" %%i in ('dir /s /-c "%HF_CACHE%" ^| findstr "个文件"') do set SIZE=%%i
echo 缓存大小约 %SIZE% bytes

echo 正在打包（大文件请耐心等待）...
powershell -Command "Compress-Archive -Path '%HF_CACHE%\*' -DestinationPath '%OUTPUT%' -Force"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   导出完成！
    echo ========================================
    echo.
    echo 文件: %OUTPUT%
    echo.
    echo 目标电脑使用方式:
    echo   1. 将 docling-models.zip 复制到目标电脑
    echo   2. 解压到 %%USERPROFILE%%\.cache\huggingface\hub\
    echo   3. 运行 install.bat 安装（会自动跳过模型下载）
    echo.
) else (
    echo [错误] 打包失败
)

pause
