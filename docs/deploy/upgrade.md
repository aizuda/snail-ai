# 升级指南

本文档介绍 Snail AI 的版本升级流程，包括升级准备、数据库备份、配置变更、验证和回滚。内容以当前仓库的 `script/` 目录和 `snail-ai-starter/src/main/resources/application.yml` 为准。

## 概览

Snail AI 0.0.6 当前提供初始化脚本：

| 数据库 | 初始化脚本 |
|--------|------------|
| MySQL | `script/sql/snail_ai_schema.sql` |
| PostgreSQL | `script/sql/snail_ai_schema_pgsql.sql` |

如果某个版本发布了独立升级脚本，请以对应 Release Notes 为准；不要假设仓库中一定存在 `script/sql/migration/` 目录。

## 升级原则

1. 先备份再升级，数据库、向量库、资源文件和加密密钥都要备份。
2. 先测试再生产，生产升级前先在测试环境演练。
3. Server 与 Agent Client 尽量保持相同版本。
4. 阅读 [更新日志](/changelog)，重点关注 Breaking Changes、配置项变更和数据库结构变更。
5. 不跨多个大版本直接升级，除非目标版本说明明确支持。

## 升级前准备

### 备份关系型数据库

MySQL：

```bash
mysqldump -u root -p --single-transaction --routines --triggers snail_ai > snail_ai_backup_$(date +%Y%m%d_%H%M%S).sql
```

PostgreSQL：

```bash
pg_dump -U postgres -Fc snail_ai > snail_ai_backup_$(date +%Y%m%d_%H%M%S).dump
```

SQL Server、达梦、MariaDB 仅作为规划或适配方向，不放入当前版本可直接执行的升级主流程。

### 备份向量与检索数据

PgVector 通常随 PostgreSQL 一起备份：

```bash
pg_dump -U postgres -Fc snail_ai_vector > snail_ai_vector_backup_$(date +%Y%m%d_%H%M%S).dump
```

Milvus 可使用官方备份工具或集群快照方案。Elasticsearch 可使用 Snapshot API 或存储层快照。

### 备份文件资源

本地资源目录：

```bash
tar -czf snail_ai_resource_$(date +%Y%m%d_%H%M%S).tar.gz ./upload/resource ./upload/skills
```

MinIO：

```bash
mc mirror minio/snail-ai /backup/snail-ai-resource/
```

### 备份加密密钥

生产环境必须备份：

```bash
SNAIL_AI_CRYPTO_KEY
SNAIL_AI_CRYPTO_IV
```

这些密钥用于解密模型 API Key 等敏感数据。密钥丢失或更换后，历史密文可能无法解密。

### 检查当前版本

```bash
curl http://localhost:8900/snail-ai/actuator/info
java -jar snail-ai-starter.jar --version
```

## 升级步骤

### JAR 部署升级

```bash
# 1. 停止当前服务
systemctl stop snail-ai-server

# 2. 备份旧 JAR
cp /opt/snail-ai/snail-ai-starter.jar /opt/snail-ai/snail-ai-starter.jar.bak

# 3. 替换新 JAR
cp /path/to/new/snail-ai-starter.jar /opt/snail-ai/snail-ai-starter.jar

# 4. 如 Release Notes 要求，先执行对应数据库变更脚本
# MySQL 示例
# mysql -u root -p snail_ai < script/sql/xxx.sql
# PostgreSQL 示例
# psql -U postgres -d snail_ai -f script/sql/xxx.sql

# 5. 启动服务
systemctl start snail-ai-server

# 6. 查看日志
journalctl -u snail-ai-server -f
```

### 依赖组件升级

仓库中的 Compose 文件主要用于 Elasticsearch、MinIO、Milvus、PgVector 等依赖组件：

```bash
docker compose -f script/docker/docker-compose.yaml pull
docker compose -f script/docker/docker-compose.yaml up -d
docker compose -f script/docker/docker-compose.yaml ps
```

当前 Compose 文件不直接包含 Snail AI Server 容器。Server 升级请按 JAR、镜像或你自己的部署方式执行。

### Agent Client 升级

升级 Server 后，建议同步升级所有 Agent Client 节点：

1. 更新 Client 依赖或部署包版本。
2. 逐个节点下线、替换、启动。
3. 在管理后台确认节点重新上线。
4. 验证对话、工具、MCP 和模型调用链路。

Client 连接配置示例：

```yaml
snail-ai:
  app-id: ${APP_ID}
  token: ${APP_TOKEN}
  server:
    host: ${SERVER_HOST}
    port: 18888
```

## 配置变更检查

当前默认关键配置：

```yaml
server:
  port: 8900
  servlet:
    context-path: /snail-ai

snail-ai:
  server:
    grpc-port: 18888
  resource:
    storage-type: LOCAL
    upload-dir: ./upload/resource
  memory:
    short-term:
      store-type: memory
```

升级时重点检查：

| 配置 | 检查点 |
|------|--------|
| `server.port` | 默认 HTTP 端口为 `8900` |
| `server.servlet.context-path` | 默认上下文路径为 `/snail-ai` |
| `snail-ai.server.grpc-port` | 默认 gRPC 端口为 `18888` |
| `spring.datasource.*` | 数据库地址、账号、驱动是否匹配 MySQL 或 PostgreSQL |
| `snail-ai.crypto.*` | 生产环境密钥是否与旧版本保持一致 |
| `snail-ai.resource.*` | 本地资源目录或 MinIO 配置是否可访问 |
| `snail-ai.memory.short-term.store-type` | 生产环境是否需要使用 `db` |

## 数据库变更处理

当前仓库提供初始化脚本，不应把初始化脚本直接用于已有生产库覆盖导入。升级已有环境时：

1. 阅读目标版本 Release Notes。
2. 判断是否有增量 SQL 或数据修复脚本。
3. 在测试库恢复生产备份后演练。
4. 记录每条 SQL 的执行结果。
5. 再在生产环境执行。

如没有官方增量脚本，但 schema 已发生变化，建议用测试库比对新旧结构后人工生成迁移脚本，并纳入变更评审。

## 升级后验证

```bash
curl http://localhost:8900/snail-ai/actuator/health
curl http://localhost:8900/snail-ai/actuator/health/db
```

功能检查：

- 管理后台可以登录。
- 模型配置可以解密并调用。
- 智能体可以正常对话。
- 流式 SSE 可以持续返回。
- Agent Client 节点在线。
- RAG 文档上传、解析、检索链路正常。
- OpenAPI 使用 `Snail-Ai-App-Id` 和 `Snail-Ai-Token` 可以调用。

## 回滚方案

### JAR 回滚

```bash
systemctl stop snail-ai-server
cp /opt/snail-ai/snail-ai-starter.jar.bak /opt/snail-ai/snail-ai-starter.jar
# 如升级已修改数据库，先按备份恢复数据库
systemctl start snail-ai-server
```

### MySQL 恢复

```bash
mysql -u root -p snail_ai < /backup/snail_ai_backup_YYYYMMDD_HHMMSS.sql
```

### PostgreSQL 恢复

```bash
pg_restore -U postgres -d snail_ai --clean /backup/snail_ai_backup_YYYYMMDD_HHMMSS.dump
```

### 文件资源恢复

```bash
tar -xzf snail_ai_resource_YYYYMMDD_HHMMSS.tar.gz -C /opt/snail-ai/
```

## 升级检查清单

- [ ] 已阅读目标版本更新日志。
- [ ] 已备份 MySQL 或 PostgreSQL 数据库。
- [ ] 已备份 PgVector、Milvus 或 Elasticsearch 数据。
- [ ] 已备份本地资源目录或 MinIO Bucket。
- [ ] 已备份 `SNAIL_AI_CRYPTO_KEY` 和 `SNAIL_AI_CRYPTO_IV`。
- [ ] 已在测试环境验证升级。
- [ ] 已准备回滚 JAR、数据库和文件资源。
- [ ] 已确认 Server 与 Agent Client 版本兼容。
- [ ] 已验证健康检查、登录、对话、RAG 和 OpenAPI。

## 相关源码

- `snail-ai-starter/src/main/resources/application.yml`
- `script/sql/snail_ai_schema.sql`
- `script/sql/snail_ai_schema_pgsql.sql`
- `script/docker/docker-compose.yaml`
- `docs/changelog.md`

## 下一步

- [更新日志](/changelog)
- [配置参考](/deploy/configuration)
- [故障排除](/deploy/troubleshooting)
