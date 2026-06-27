# 配置参考

本文档以当前源码中的 `snail-ai-starter/src/main/resources/application.yml` 为准，整理 Snail AI 0.0.6 后端服务的主要配置项。

## 概览

配置重点包括 HTTP 服务、关系型数据库、gRPC、资源存储、短期记忆、模型调用和敏感信息加密。默认访问地址为：

```text
http://localhost:8900/snail-ai
```

## 当前支持状态

| 能力 | 状态 | 对应源码或脚本 |
|------|------|----------------|
| HTTP 服务 | 已支持，默认 `8900` + `/snail-ai` | `snail-ai-starter/src/main/resources/application.yml` |
| gRPC Server | 已支持，默认 `18888` | `snail-ai-starter/src/main/resources/application.yml` |
| MySQL | 已支持 | `script/sql/snail_ai_schema.sql` |
| PostgreSQL | 已支持 | `script/sql/snail_ai_schema_pgsql.sql` |
| PgVector | 已支持 | `script/docker/docker-compose.yaml`、Spring AI PgVector 配置 |
| Milvus | 已支持 | `script/docker/docker-compose.yaml` |
| Elasticsearch | 已支持 | `script/docker/docker-compose.yaml` |
| MinIO / 本地资源存储 | 已支持 | `snail-ai.resource.*` |
| 短期记忆 | 已支持，`memory` / `db` | `snail-ai.memory.short-term.store-type` |
| SQL Server / 达梦 / MariaDB | 规划或适配方向 | 不建议按当前文档直接部署 |

## 快速示例

```yaml
server:
  port: 8900
  servlet:
    context-path: /snail-ai

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/snail_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

snail-ai:
  server:
    grpc-port: 18888
  skill:
    upload-dir: ./upload/skills
  crypto:
    secret-key: ${SNAIL_AI_CRYPTO_KEY:0123456789abcdef0123456789abcdef}
    iv: ${SNAIL_AI_CRYPTO_IV:fedcba9876543210fedcba9876543210}
  resource:
    storage-type: LOCAL
    upload-dir: ./upload/resource
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: snail-ai
  memory:
    short-term:
      store-type: memory
```

## HTTP 与 gRPC

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8900` | Admin API、OpenAPI、SSE 流式响应共用 HTTP 端口 |
| `server.servlet.context-path` | `/snail-ai` | HTTP 上下文路径 |
| `snail-ai.server.grpc-port` | `18888` | Server 端 gRPC 监听端口，用于 Client 注册、心跳和任务分发 |

## 数据库连接

### MySQL

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/snail_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

初始化脚本：

```bash
mysql -u root -p snail_ai < script/sql/snail_ai_schema.sql
```

### PostgreSQL

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/snail_ai?currentSchema=public
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
```

初始化脚本：

```bash
psql -U postgres -d snail_ai -f script/sql/snail_ai_schema_pgsql.sql
```

## 向量与检索存储

### PgVector

PgVector 可作为 PostgreSQL 扩展使用。首次使用前需要在目标数据库启用扩展：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

常见配置：

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        enabled: true
        dimensions: 1536
        distance-type: cosine_distance
        index-type: hnsw
```

向量维度必须与 Embedding 模型输出维度一致。

### Milvus 和 Elasticsearch

开发或验证环境可使用仓库中的 Compose 文件启动依赖组件：

```bash
docker compose -f script/docker/docker-compose.yaml up -d
```

当前 Compose 文件主要提供 Elasticsearch、MinIO、Milvus 和 PgVector 等依赖组件，不直接包含 Snail AI Server 容器。

## 资源存储

### 本地存储

```yaml
snail-ai:
  resource:
    storage-type: LOCAL
    upload-dir: ./upload/resource
```

本地存储适合开发测试或单节点部署。生产环境多节点部署时，建议使用对象存储。

### MinIO

```yaml
snail-ai:
  resource:
    storage-type: MINIO
    minio:
      endpoint: http://localhost:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket: snail-ai
```

## Skill 上传目录

```yaml
snail-ai:
  skill:
    upload-dir: ./upload/skills
```

该目录用于保存上传或加载的技能资源。生产环境建议挂载到持久化磁盘。

## 短期记忆

```yaml
snail-ai:
  memory:
    short-term:
      store-type: memory
```

| 值 | 说明 |
|----|------|
| `memory` | 存储在应用内存中，重启后丢失，适合开发测试 |
| `db` | 存储在数据库中，重启后保留，适合生产或多节点场景 |

当前文档不把长期记忆、向量化记忆或完整 Memory Admin API 描述为已完整落地能力；相关能力请以源码实现为准。

## 加密配置

Snail AI 使用对称加密保护模型 API Key 等敏感数据：

```yaml
snail-ai:
  crypto:
    secret-key: ${SNAIL_AI_CRYPTO_KEY:0123456789abcdef0123456789abcdef}
    iv: ${SNAIL_AI_CRYPTO_IV:fedcba9876543210fedcba9876543210}
```

生产环境必须通过环境变量注入并妥善备份密钥：

```bash
export SNAIL_AI_CRYPTO_KEY=your_32_char_secret_key
export SNAIL_AI_CRYPTO_IV=your_32_char_initial_vector
```

密钥一旦用于加密已有数据，不要随意更换，否则历史密文可能无法解密。

## 模型默认配置

模型配置通过后台模型管理和运行时配置共同生效。当前源码已落地的模型调用规格应按以下范围理解：

| 能力 | 支持方式 |
|------|----------|
| Chat | OpenAI-compatible Chat |
| Embedding | OpenAI-compatible Embedding |
| Rerank | Qwen/HTTP Rerank |

如果 Claude、Gemini、Ollama、火山引擎等服务提供 OpenAI-compatible 接口，可以按兼容端点方式验证接入；不要理解为当前源码内置的一等 Provider。

## 环境变量覆盖

Spring Boot 配置可通过环境变量覆盖，常用项如下：

| YAML 配置路径 | 环境变量名 |
|---------------|------------|
| `server.port` | `SERVER_PORT` |
| `server.servlet.context-path` | `SERVER_SERVLET_CONTEXT_PATH` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |
| `snail-ai.server.grpc-port` | `SNAIL_AI_SERVER_GRPC_PORT` |
| `snail-ai.resource.storage-type` | `SNAIL_AI_RESOURCE_STORAGE_TYPE` |
| `snail-ai.resource.upload-dir` | `SNAIL_AI_RESOURCE_UPLOAD_DIR` |
| `snail-ai.crypto.secret-key` | `SNAIL_AI_CRYPTO_SECRET_KEY` |
| `snail-ai.crypto.iv` | `SNAIL_AI_CRYPTO_IV` |
| `snail-ai.memory.short-term.store-type` | `SNAIL_AI_MEMORY_SHORT_TERM_STORE_TYPE` |

## 日志配置

```yaml
logging:
  level:
    root: INFO
    com.aizuda.snail.ai: INFO
    org.springframework.ai: INFO
    io.grpc: WARN
  file:
    name: /app/logs/snail-ai.log
```

生产环境建议开启日志轮转，避免日志文件占满磁盘。

## 相关源码

- `snail-ai-starter/src/main/resources/application.yml`
- `pom.xml`
- `script/docker/docker-compose.yaml`
- `script/sql/snail_ai_schema.sql`
- `script/sql/snail_ai_schema_pgsql.sql`
