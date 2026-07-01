# Docling API — 文档转换 REST 服务

基于 IBM Docling 的文档解析服务，支持 PDF、DOCX、PPTX、XLSX、HTML、图片等格式转换为 Markdown/JSON/HTML/纯文本。

> **在 Snail AI 项目中使用**: 本项目已集成到 `tools/docling-api/`，可通过 Claude Code skill `/docling-manager` 管理服务。

## 支持格式

PDF、DOCX、PPTX、XLSX、HTML、图片（含 OCR）、Markdown、CSV、XML 等。

---

## 快速开始

### 方式一：Claude Code Skill（推荐）

在项目根目录下，通过 Claude Code 管理：

```
/docling-manager start     # 后台启动
/docling-manager stop      # 停止
/docling-manager status    # 查看状态
/docling-manager restart   # 重启
```

### 方式二：双击启动

```
双击 start.bat → 命令行窗口运行，关闭窗口即停止
```

### 方式三：后台启动（无窗口）

```
双击 server\start-background.bat → 后台运行，无控制台窗口
停止: taskkill /f /im pythonw.exe
```

### 方式四：注册 Windows 服务（开机自启，推荐生产环境）

```
右键 server\install-service.bat → 以管理员身份运行
```

服务会出现在 `services.msc`（服务名: DoclingAPI），开机自动启动。

---

## 验证服务

```
curl http://localhost:5100/health
```

浏览器打开: http://localhost:5100/docs （Swagger 交互式文档）

---

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/convert` | POST | 同步转换：上传文件或 URL，返回 md/json/html/text |
| `/v1/convert/file/async` | POST | 异步提交任务（Snail AI 对接用） |
| `/v1/status/poll/{task_id}` | GET | 轮询异步任务状态 |
| `/v1/result/{task_id}` | GET | 获取异步任务结果 |

### 快速测试

```bash
# 上传文件转 Markdown
curl -F "file=@document.pdf" http://localhost:5100/convert

# URL 转 JSON
curl -F "url=https://example.com/doc.pdf" -F "to=json" http://localhost:5100/convert
```

---

## 部署到其他电脑

### 有网络（在线安装）

1. 复制整个 `tools/docling-api` 文件夹到目标电脑
2. 双击 `server\install.bat`（自动安装依赖 + 下载模型）
3. （可选）以管理员身份运行 `server\install-service.bat` 注册服务

### 无网络（离线安装）

1. 在本机先跑一次 `server\model-cache-export.bat`，得到 `docling-models.zip`
2. 将 `docling-api` 文件夹 + `docling-models.zip` 复制到目标电脑
3. 在目标电脑上将 `docling-models.zip` 解压到 `%USERPROFILE%\.cache\huggingface\hub\`
4. Python 3.9+ 需提前安装好（离线包在 https://www.python.org/downloads/）
5. 双击 `server\install.bat`

---

## 关机后重启

| 启动方式 | 关机后行为 |
|----------|-----------|
| `/docling-manager start` | 需手动重新启动 |
| `start.bat` | 需手动重新双击 |
| `start-background.bat` | 需手动重新双击 |
| Windows 服务 (`install-service.bat`) | **自动启动**，无需人工干预 |

---

## 认证（可选）

设置环境变量 `DOCLING_API_KEY` 后重启服务：

```bat
setx DOCLING_API_KEY "your-random-secret-key"
```

之后所有 `/convert` 和 `/v1/*` 请求需要带 header: `X-API-Key: your-random-secret-key`（`/health` 和 `/docs` 除外）。

---

## 业务系统对接

详见 [server/API.md](server/API.md) — 包含完整的请求/响应示例、错误码、cURL/Java/Python/JavaScript 代码示例、生产环境 Nginx 配置建议。

---

## 项目结构

```
tools/docling-api/
├── start.bat                    # 前台启动
├── README.md                    # 本文档
└── server/
    ├── main.py                  # FastAPI 服务（v2.1，含异步三步接口）
    ├── requirements.txt         # Python 依赖
    ├── API.md                   # 业务系统对接文档
    ├── install.bat              # 一键安装脚本
    ├── install-service.bat      # Windows 服务注册（需管理员）
    ├── start-background.bat     # 后台启动（无窗口）
    ├── model-cache-export.bat   # 导出模型缓存（离线部署用）
    └── client/
        └── DoclingClient.java  # Java 客户端（零依赖）
```
