# Docker 部署

本文介绍仓库当前提供的 Docker Compose 脚本。当前 Compose 文件可启动 Snail AI Server、基础依赖组件和 RAG 工具服务。

## 当前支持状态

| 组件 | 状态 | 脚本/来源 |
|------|------|-----------|
| Elasticsearch | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| MinIO | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| Milvus | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| PgVector | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| Docling API | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| PaddleOCR API | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| Snail AI Server 镜像 | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| MySQL 初始化脚本 | 已提供 | `docs/sql/snail_ai_schema.sql` |
| PostgreSQL 初始化脚本 | 已提供 | `docs/sql/snail_ai_schema_pgsql.sql` |

## 前提条件

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | v2.0+ | 推荐使用 `docker compose` V2 命令 |

## 启动 Compose 服务

在仓库根目录执行：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d
```

或进入脚本目录执行：

```bash
cd docs/docker
docker compose up -d
```

默认会启动以下服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| Snail AI Server | `8900`、`18888` | 后端 HTTP 与 Agent Client gRPC |
| MySQL | `13306 -> 3306` | 默认业务库 |
| Elasticsearch | `9200`、`9300` | 向量/检索存储可选项 |
| MinIO | `9000`、`9001` | 对象存储 |
| Milvus | `19530`、`9091` | 向量数据库 |
| PgVector | `15432 -> 5432` | PostgreSQL + pgvector 镜像 |
| Docling API | `5100` | 文档转换 REST 服务 |
| PaddleOCR API | `8866` | 图片 OCR REST 服务 |

如果只启动 RAG 工具服务：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d --build docling-api paddle-ocr-api
```

Docling 首次需要下载 PDF/OCR 模型时：

```bash
DOCLING_DOWNLOAD_MODELS=true docker compose -f docs/docker/docker-compose.yaml up -d docling-api
```

## 初始化数据库

Compose 中的 PgVector 容器可用于 PostgreSQL/PgVector 场景。业务库使用 MySQL 时请自行启动 MySQL 8.0+，并导入：

```bash
mysql -u root -p snail_ai < docs/sql/snail_ai_schema.sql
```

业务库使用 PostgreSQL 时导入：

```bash
psql -U postgres -d snail_ai -f docs/sql/snail_ai_schema_pgsql.sql
```

## 初始化数据库（Docker 容器内）

如果 MySQL 通过 `script/docker/docker-compose.yml` 启动，可以直接在容器内执行：

```bash
docker exec -i mysql mysql -uroot -p123456 snail_ai < docs/sql/snail_ai_schema.sql
```

## 拉取 Ollama 模型（演示环境）

`script/docker/docker-compose.yml` 中包含 Ollama 服务，启动后需手动拉取模型：

```bash
# 拉取 Embedding 模型（约 274MB）
docker exec ollama ollama pull nomic-embed-text

# 可选：拉取轻量聊天模型（约 352MB）
docker exec ollama ollama pull qwen2:0.5b

# 查看已安装模型
docker exec ollama ollama list
```

## 启动 Snail AI Server

Compose 文件已包含 Snail AI Server 镜像。也可在本机用 Maven 启动：

```bash
mvn spring-boot:run -pl snail-ai-starter
```

服务默认地址：

```text
http://localhost:8900/snail-ai
```

gRPC 默认端口：

```text
18888
```

## 常用运维命令

```bash
# 查看依赖组件状态
docker compose -f docs/docker/docker-compose.yaml ps

# 查看日志
docker compose -f docs/docker/docker-compose.yaml logs -f

# 重启依赖组件
docker compose -f docs/docker/docker-compose.yaml restart

# 停止依赖组件
docker compose -f docs/docker/docker-compose.yaml down

# 停止并删除依赖组件数据卷（慎用）
docker compose -f docs/docker/docker-compose.yaml down -v
```

## 配置对应关系

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8900` | HTTP 服务端口 |
| `server.servlet.context-path` | `/snail-ai` | HTTP 上下文路径 |
| `snail-ai.server.grpc-port` | `18888` | Agent Client gRPC 端口 |
| `snail-ai.resource.storage-type` | `LOCAL` | 可选 `LOCAL` / `MINIO` |
| `snail-ai.memory.short-term.store-type` | `memory` | 可选 `memory` / `db` |

## 相关源码与脚本

- `docs/docker/docker-compose.yaml`
- `docs/docker/elasticsearch/Dockerfile`
- `docs/sql/snail_ai_schema.sql`
- `docs/sql/snail_ai_schema_pgsql.sql`
- `snail-ai-starter/src/main/resources/application.yml`
