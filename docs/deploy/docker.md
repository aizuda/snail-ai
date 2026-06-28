# Docker 部署

本文介绍仓库当前提供的 Docker Compose 脚本。0.0.6 版本中，Docker 与 SQL 脚本已经从 `docs/` 移到根目录 `script/`；当前 Compose 文件主要用于启动 Snail AI 依赖组件。

## 当前支持状态

| 组件 | 状态 | 脚本/来源 |
|------|------|-----------|
| Elasticsearch | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| MinIO | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| Milvus | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| PgVector | 已提供 Compose 服务 | `docs/docker/docker-compose.yaml` |
| Snail AI Server 镜像 | 当前 Compose 未包含 | 使用 Maven/JAR 或自行扩展 Compose |
| MySQL 初始化脚本 | 已提供 | `docs/sql/snail_ai_schema.sql` |
| PostgreSQL 初始化脚本 | 已提供 | `docs/sql/snail_ai_schema_pgsql.sql` |

## 前提条件

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | v2.0+ | 推荐使用 `docker compose` V2 命令 |

## 启动依赖组件

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
| Elasticsearch | `9200`、`9300` | 向量/检索存储可选项 |
| MinIO | `9000`、`9001` | 对象存储 |
| Milvus | `19530`、`9091` | 向量数据库 |
| PgVector | `15432 -> 5432` | PostgreSQL + pgvector 镜像 |

## 初始化数据库

Compose 中的 PgVector 容器可用于 PostgreSQL/PgVector 场景。业务库使用 MySQL 时请自行启动 MySQL 8.0+，并导入：

```bash
mysql -u root -p snail_ai < docs/sql/snail_ai_schema.sql
```

业务库使用 PostgreSQL 时导入：

```bash
psql -U postgres -d snail_ai -f docs/sql/snail_ai_schema_pgsql.sql
```

## 启动 Snail AI Server

当前仓库 Compose 文件不直接包含 Snail AI Server 容器。可先用 Maven 在本机启动：

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
