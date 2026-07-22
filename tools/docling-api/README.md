# Docling API — 文档转换 REST 服务

基于 IBM Docling 的文档解析服务，支持 PDF、DOCX、PPTX、XLSX、HTML、图片等格式转换为 Markdown/JSON/HTML/纯文本。

## 脚本入口

本目录只保留两个对外脚本：

| 脚本 | 用途 | 何时运行 |
|------|------|----------|
| `setup-docling-api-env.bat` | 安装 Python 依赖，并下载 PDF 解析模型 | 新机器首次部署、依赖更新、模型缓存缺失 |
| `start-docling-api.bat` | 启动 Docling API 服务 | 环境准备完成后每次启动 |

启动地址：

```txt
http://localhost:5100
```

Swagger 文档：

```txt
http://localhost:5100/docs
```

健康检查：

```bat
curl http://localhost:5100/health
```

## Docker 启动

也可以直接使用本仓库提供的 Docker Compose 启动 Docling API：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d --build docling-api
```

默认端口仍为 `5100`，Docling 模型目录挂载到 Compose 命名卷 `docling-models`。如果希望容器启动时自动下载 PDF/OCR 模型：

```bash
DOCLING_DOWNLOAD_MODELS=true docker compose -f docs/docker/docker-compose.yaml up -d docling-api
```

网络需要使用 HuggingFace 镜像时：

```bash
DOCLING_DOWNLOAD_MODELS=true DOCLING_HF_ENDPOINT=https://hf-mirror.com docker compose -f docs/docker/docker-compose.yaml up -d docling-api
```

单独构建镜像：

```bash
docker build -t snail-ai/docling-api:latest tools/docling-api/server
docker run --rm -p 5100:5100 -v docling-models:/models/docling snail-ai/docling-api:latest
```

## 新机器安装

### 在线机器

目标机器能访问 PyPI 和 HuggingFace 时，按下面顺序执行：

1. 安装 Python 3.9+，安装时勾选 `Add Python to PATH`
2. 复制整个 `tools/docling-api` 目录到目标机器
3. 双击 `setup-docling-api-env.bat`
4. 双击 `start-docling-api.bat`
5. 浏览器打开 `http://localhost:5100/docs` 验证服务
6. 用一个 PDF 试解析，确认 PDF 模型和 OCR 都可用

`setup-docling-api-env.bat` 会做两件事：

```bat
python -m pip install -r server\requirements.txt
python -m docling.cli.models download layout tableformer rapidocr -o "%USERPROFILE%\.cache\docling\models"
```

模型下载会先尝试默认 HuggingFace；如果连接超时，脚本会自动使用 `https://hf-mirror.com` 重试。若目标网络有公司内网镜像，可先设置：

```bat
set DOCLING_HF_ENDPOINT=https://your-huggingface-mirror.example.com
setup-docling-api-env.bat
```

### 离线机器

离线部署需要同时准备两类东西：

- Python 依赖包：`server\requirements.txt` 里的依赖
- Docling PDF/OCR 模型缓存：`%USERPROFILE%\.cache\docling\models`

推荐流程：

1. 在一台能联网的机器上安装 Python 3.9+
2. 复制 `tools/docling-api`
3. 双击 `setup-docling-api-env.bat`
4. 用 PDF 成功解析一次，确保模型缓存完整
5. 将联网机器上的 `%USERPROFILE%\.cache\docling\models` 打包
6. 在离线目标机器上解压到同一路径：`%USERPROFILE%\.cache\docling\models`
7. 在离线目标机器安装 Python 3.9+
8. 从内网 PyPI 或本地 wheel 包安装 `server\requirements.txt` 中的依赖
9. 双击 `start-docling-api.bat`

打包模型缓存可用 PowerShell：

```powershell
Compress-Archive -Path "$env:USERPROFILE\.cache\docling\models\*" -DestinationPath ".\docling-models.zip" -Force
```

目标机器解压：

```powershell
Expand-Archive -Path ".\docling-models.zip" -DestinationPath "$env:USERPROFILE\.cache\docling\models" -Force
```

离线安装依赖时，如果有本地 wheel 目录：

```bat
cd /d tools\docling-api\server
python -m pip install --no-index --find-links D:\wheelhouse -r requirements.txt
```

## PDF 与 Word 解析差异

DOCX/PPTX/XLSX 等 Office 文件通常走 Docling 的 `SimplePipeline`，主要读取文档内部结构和文本内容。

PDF 会走 `StandardPdfPipeline`，会额外初始化版面识别、表格识别和 OCR 模型。扫描件 PDF 或图片型 PDF 必须依赖 OCR；即使普通 PDF，也可能在版面分析阶段加载本地模型。因此会出现 Word 解析正常、PDF 首次解析才暴露 Python 依赖或模型缓存问题的情况。

当前服务固定使用 RapidOCR 的 `onnxruntime` 后端，避免 Docling 自动回退到 RapidOCR `torch` 后端后触发：

```txt
Unsupported configuration: torch.PP-OCRv6.det.small
```

如果看到这个错误，说明当前运行环境没有安装或没有加载 `onnxruntime`。处理方式：

```bat
setup-docling-api-env.bat
```

然后重启 Docling API。

如果看到 HuggingFace `ConnectTimeout` 或 “cannot find the appropriate snapshot folder”，说明 PDF 布局/表格模型没有下载到本机缓存。联网机器重新执行 `setup-docling-api-env.bat`，脚本会在直连失败后自动尝试 `https://hf-mirror.com`；离线机器按上面的离线流程复制 `%USERPROFILE%\.cache\docling\models`。

`start-docling-api.bat` 会把 `DOCLING_MODEL_DIR` 固定为 `%USERPROFILE%\.cache\docling\models`，服务端转换 PDF 时会直接使用这个本地模型目录，避免运行过程中再次去 HuggingFace 或 ModelScope 查找模型。

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/convert` | POST | 同步转换：上传文件或 URL，返回 md/json/html/text |
| `/v1/convert/file/async` | POST | 异步提交任务（Snail AI 对接用） |
| `/v1/status/poll/{task_id}` | GET | 轮询异步任务状态 |
| `/v1/result/{task_id}` | GET | 获取异步任务结果 |

快速测试：

```bash
curl -F "file=@document.pdf" http://localhost:5100/convert
curl -F "url=https://example.com/doc.pdf" -F "to=json" http://localhost:5100/convert
```

## 认证（可选）

设置环境变量 `DOCLING_API_KEY` 后重启服务：

```bat
setx DOCLING_API_KEY "your-random-secret-key"
```

之后所有 `/convert` 和 `/v1/*` 请求需要带 header: `X-API-Key: your-random-secret-key`，`/health` 和 `/docs` 除外。

## 项目结构

```txt
tools/docling-api/
├── setup-docling-api-env.bat   # 安装依赖并下载 PDF 模型
├── start-docling-api.bat       # 启动 Docling API
├── README.md
└── server/
    ├── Dockerfile              # Docker 镜像构建文件
    ├── docker-entrypoint.sh    # Docker 启动入口
    ├── main.py                 # FastAPI 服务
    ├── requirements.txt        # Python 依赖
    ├── API.md                  # 业务系统对接文档
    ├── tests/                  # 单元测试
    └── client/
        └── DoclingClient.java  # Java 客户端
```
