# PaddleOCR API

本目录封装一个独立的 PaddleOCR HTTP 服务，用于 Snail AI 文档图片 OCR 增强。

服务默认地址：

```text
http://127.0.0.1:8866
```

后端默认调用：

```text
POST http://127.0.0.1:8866/ocr
POST http://127.0.0.1:8866/ocr/batch
```

Snail AI batch image OCR calls `/ocr/batch` first. The single-image `/ocr` endpoint remains available for compatibility and fallback.
The batch endpoint sends valid images to PaddleOCR in one `predict([...])` call, while invalid images are returned as failed items in their original positions.

## GPU Quick Start

Use this path on machines with an NVIDIA GPU and a working `nvidia-smi` command:

```bat
server\install-gpu.bat
start-gpu.bat
```

`server\install-gpu.bat` installs `paddlepaddle-gpu==3.2.2` from the PaddlePaddle CUDA 12.6 package index. `start-gpu.bat` sets:

```bat
PADDLE_OCR_DEVICE=gpu:0
PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE=8
PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE=8
```

Use `server\start-background-gpu.bat` for GPU background startup. It opens a minimized Python console for the OCR service; keep that window running while Snail AI parses documents. The original `server\install.bat` and `start.bat` remain the CPU-compatible path.

## Docker 启动

也可以直接使用本仓库提供的 Docker Compose 启动 PaddleOCR API：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d --build paddle-ocr-api
```

默认端口仍为 `8866`，模型缓存会保存在 Compose 命名卷中。单独构建镜像：

```bash
docker build -t snail-ai/paddle-ocr-api:latest tools/paddle-ocr-api/server
docker run --rm -p 8866:8866 snail-ai/paddle-ocr-api:latest
```

如果需要构建 GPU 版镜像，可在构建时切换 PaddlePaddle 包源，并在运行时设置 `PADDLE_OCR_DEVICE=gpu:0`：

```bash
docker build \
  --build-arg PADDLEPADDLE_PACKAGE=paddlepaddle-gpu \
  --build-arg PADDLEPADDLE_INDEX=https://www.paddlepaddle.org.cn/packages/stable/cu126/ \
  --build-arg PADDLEPADDLE_VERSION=3.2.2 \
  -t snail-ai/paddle-ocr-api:gpu \
  tools/paddle-ocr-api/server
```

## 快速开始

### 1. 安装依赖

双击或执行：

```bat
server\install.bat
```

安装脚本会在 `server\.venv` 中创建独立 Python 虚拟环境，并安装 CPU 版 PaddlePaddle、PaddleOCR、FastAPI。

### 2. 启动服务

前台启动：

```bat
start.bat
```

后台启动：

```bat
server\start-background.bat
```

### 3. 检查服务

```bash
curl http://127.0.0.1:8866/health
```

浏览器打开 Swagger：

```text
http://127.0.0.1:8866/docs
```

## Snail AI 配置

后端配置：

```yaml
snail-ai:
  rag:
    docling:
      paddle-ocr-enabled: true
      paddle-ocr-url: http://127.0.0.1:8866/ocr
      paddle-ocr-timeout-millis: 180000
      vision-ocr-fallback-enabled: false
```

前端知识库解析配置中开启 `图片 OCR 增强`。

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PADDLE_OCR_HOST` | `0.0.0.0` | 服务监听地址 |
| `PADDLE_OCR_PORT` | `8866` | 服务端口 |
| `PADDLE_OCR_LANG` | `ch` | OCR 语言，中文/英文建议用 `ch` |
| `PADDLE_OCR_DEVICE` | 空 | PaddleOCR 推理设备，例如 `cpu`、`gpu:0` |
| `PADDLE_OCR_CPU_THREADS` | `1` | CPU 推理线程数，Docker CPU 默认固定为 1，避免 ARM 环境下 PaddlePaddle 原生推理段错误 |
| `PADDLE_OCR_USE_TEXTLINE_ORIENTATION` | `true` | 是否启用文字行方向分类；关闭可提速但可能影响旋转文字 |
| `PADDLE_OCR_TEXT_RECOGNITION_BATCH_SIZE` | 空 | 文本识别模型 batch size，CPU 可从 6/8/12 小步试起 |
| `PADDLE_OCR_TEXTLINE_ORIENTATION_BATCH_SIZE` | 空 | 文字行方向分类模型 batch size |
| `PADDLE_OCR_API_KEY` | 空 | 设置后 `/ocr` 和 `/ocr/batch` 请求必须带 `X-API-Key` |
| `PADDLE_OCR_MAX_IMAGE_MB` | `20` | 单张图片最大大小 |
| `PADDLE_OCR_MAX_CONCURRENCY` | `1` | OCR 最大并发数 |
| `PADDLE_OCR_QUEUE_TIMEOUT_SECONDS` | `300` | OCR 忙时单个请求最长排队等待时间 |
| `PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK` | `True` | 跳过模型源连通性探测，减少首次加载等待 |
| `PADDLE_FLAGS_USE_MKLDNN` | `0` | 映射到容器内 `FLAGS_use_mkldnn`，Docker CPU 默认禁用 MKLDNN |
| `PADDLE_OMP_NUM_THREADS` | `1` | 映射到容器内 `OMP_NUM_THREADS` |
| `PADDLE_OPENBLAS_NUM_THREADS` | `1` | 映射到容器内 `OPENBLAS_NUM_THREADS` |
| `PADDLE_MKL_NUM_THREADS` | `1` | 映射到容器内 `MKL_NUM_THREADS` |

## 接口文档

详见 [server/API.md](server/API.md)。

## 目录结构

```text
tools/paddle-ocr-api/
├── start.bat
├── start-gpu.bat
├── README.md
└── server/
    ├── Dockerfile
    ├── main.py
    ├── requirements.txt
    ├── install.bat
    ├── install-gpu.bat
    ├── start-background.bat
    └── API.md
```
