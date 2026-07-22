# Snail AI Tools Docker

本目录提供两个独立工具服务的 Docker 化启动方式：

| 服务 | 说明 | 默认地址 |
|------|------|----------|
| `docling-api` | Docling 文档转换 REST 服务 | `http://localhost:5100` |
| `paddle-ocr-api` | PaddleOCR 图片 OCR REST 服务 | `http://localhost:8866` |

## 启动

```bash
docker compose -f docs/docker/docker-compose.yaml up -d --build docling-api paddle-ocr-api
```

只启动其中一个服务：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d --build docling-api
docker compose -f docs/docker/docker-compose.yaml up -d --build paddle-ocr-api
```

如需固定端口、API Key 或并发参数，可复制 `docs/docker/.env.example` 为 `docs/docker/.env`，并通过 `--env-file docs/docker/.env` 加载。

查看健康状态：

```bash
curl http://localhost:5100/health
curl http://localhost:8866/health
```

## 模型缓存

Compose 使用命名卷保存模型缓存，避免容器重建后重复下载：

| 卷 | 用途 |
|----|------|
| `docling-models` | Docling PDF/OCR 模型 |
| `paddle-ocr-cache` | PaddleOCR 模型 |
| `paddle-paddlex-cache` | PaddleX 模型 |

Docling 默认不在容器启动时下载模型。如果目标机器首次部署并且可联网，可以执行：

```bash
DOCLING_DOWNLOAD_MODELS=true docker compose -f docs/docker/docker-compose.yaml up -d docling-api
```

使用 HuggingFace 镜像：

```bash
DOCLING_DOWNLOAD_MODELS=true DOCLING_HF_ENDPOINT=https://hf-mirror.com docker compose -f docs/docker/docker-compose.yaml up -d docling-api
```

## 端口

如需修改宿主机端口：

```bash
DOCLING_PORT=15100 PADDLE_OCR_PORT=18866 docker compose -f docs/docker/docker-compose.yaml up -d docling-api paddle-ocr-api
```
