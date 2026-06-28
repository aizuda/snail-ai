# 故障排除

本文档按类别整理 Snail AI 0.0.6 使用过程中的常见问题。排查时请优先以当前源码、`application.yml` 和 `script/` 目录为准。

## 概览

默认 HTTP 访问地址为 `http://localhost:8900/snail-ai`，Server gRPC 端口为 `18888`。当前可直接使用的初始化脚本位于 `docs/sql/`，Docker Compose 依赖组件文件位于 `docs/docker/docker-compose.yaml`。

## 当前支持状态

| 能力 | 状态 | 对应源码或脚本 |
|------|------|----------------|
| Java 运行环境 | Java 21+ | `pom.xml` |
| MySQL | 已支持 | `docs/sql/snail_ai_schema.sql` |
| PostgreSQL | 已支持 | `docs/sql/snail_ai_schema_pgsql.sql` |
| PgVector / Milvus / Elasticsearch | 已支持 | `docs/docker/docker-compose.yaml` |
| OpenAPI 认证 | `Snail-Ai-App-Id` + `Snail-Ai-Token` | `OpenApiAuthInterceptor` |
| Admin 认证 | `Snail-Ai-Auth` | Admin API 登录态 |
| SQL Server / 达梦 / MariaDB | 规划或适配方向 | 不建议按当前文档直接部署 |

## 安装与依赖

### Java 版本不兼容

**现象：** 启动时报 `UnsupportedClassVersionError` 或 `class file has wrong version`。

**原因：** Snail AI 0.0.6 要求 Java 21 或更高版本。

**解决：**

```bash
java -version
```

如果低于 21，请安装 JDK 21+，并确认 `JAVA_HOME` 指向正确版本：

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

### Maven 依赖下载失败

**现象：** `mvn spring-boot:run` 时报 `Could not resolve dependencies` 或下载超时。

**解决：**

1. 检查网络连接，确认能访问 Maven Central。
2. 如在国内网络，可配置 Maven 镜像源。
3. 清理本地缓存后重试：

```bash
mvn dependency:purge-local-repository
mvn clean install -U
```

### 前端依赖安装失败

**现象：** `pnpm install` 报错或长时间卡住。

**解决：**

```bash
node -v
pnpm -v
pnpm store prune
pnpm install
```

如为网络问题，可临时设置镜像源后重试。

## 数据库问题

### 数据库连接失败

**现象：** 启动时报 `Communications link failure`、`Connection refused` 或认证失败。

**排查步骤：**

1. 确认数据库服务已启动：

```bash
# MySQL
mysqladmin ping -h localhost -u root -p

# PostgreSQL
pg_isready
```

2. 测试连接参数：

```bash
mysql -h localhost -P 3306 -u root -p snail_ai
psql -h localhost -p 5432 -U postgres -d snail_ai
```

3. 检查 `application.yml` 中的 `url`、`username`、`password`、`driver-class-name`。

### 数据库初始化脚本执行失败

**现象：** 导入 SQL 时报语法错误、表已存在或字段冲突。

**解决：**

- MySQL 使用 `docs/sql/snail_ai_schema.sql`。
- PostgreSQL 使用 `docs/sql/snail_ai_schema_pgsql.sql`。
- 不要把 MySQL 脚本导入 PostgreSQL，也不要反向导入。
- 重复导入前请确认是否需要清空数据库。

```sql
-- MySQL
DROP DATABASE IF EXISTS snail_ai;
CREATE DATABASE snail_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- PostgreSQL
DROP DATABASE IF EXISTS snail_ai;
CREATE DATABASE snail_ai WITH ENCODING 'UTF8';
```

### 数据库迁移或升级失败

**现象：** 升级后启动报字段缺失、字段重复或 SQL 执行失败。

**解决：**

1. 确认当前版本提供的是初始化脚本还是升级脚本。
2. 对生产数据先备份，再执行结构变更。
3. 本地验证升级 SQL 能重复执行或有明确的执行前置条件。
4. 如使用自定义 Flyway/Liquibase 流程，请以实际迁移表和脚本为准。

## 向量存储问题

### PgVector 扩展安装失败

**现象：** 执行 `CREATE EXTENSION vector` 报 `could not open extension control file`。

**解决：**

```bash
# Debian/Ubuntu 示例，版本需与 PostgreSQL 版本匹配
apt install postgresql-16-pgvector
```

或直接使用带 PgVector 扩展的镜像。启用扩展：

```bash
psql -U postgres -d snail_ai_vector -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### PgVector 向量维度不匹配

**现象：** 插入向量时报 `expected X dimensions, not Y`。

**原因：** 表或索引维度与 Embedding 模型输出维度不一致。

**解决：**

1. 确认 Embedding 模型输出维度。
2. 修改 PgVector 配置中的 `dimensions`。
3. 已入库数据需要重新嵌入或重建向量表。

### Milvus 连接失败

**现象：** 报 `MilvusClientException` 或 `connect to Milvus failed`。

**排查步骤：**

```bash
docker compose -f docs/docker/docker-compose.yaml ps
nc -zv localhost 19530
docker compose -f docs/docker/docker-compose.yaml logs milvus
```

确认 Milvus、etcd、MinIO 均处于运行状态，并检查应用配置中的 host 和 port。

### Elasticsearch 集群状态异常

**现象：** ES 集群状态为 `red` 或 `yellow`，检索超时。

**排查步骤：**

```bash
curl -X GET "localhost:9200/_cluster/health?pretty"
curl -X GET "localhost:9200/_cat/shards?v&h=index,shard,prirep,state,unassigned.reason"
curl -X GET "localhost:9200/_cat/allocation?v"
```

常见原因包括磁盘空间不足、节点数不足和内存不足。开发环境可将副本数设置为 0，生产环境建议规划集群容量。

## Agent Client 问题

### gRPC 连接失败

**现象：** Client 启动后无法连接 Server，报 `UNAVAILABLE: io exception` 或 `Connection refused`。

**排查步骤：**

1. 确认 Server 已启动。
2. 确认 Server 监听 `18888` 端口。
3. 从 Client 机器测试端口连通性：

```bash
nc -zv server-host 18888
```

4. 检查 Client 配置：

```yaml
snail-ai:
  server:
    host: server-host
    port: 18888
```

### 心跳超时

**现象：** 管理后台显示客户端节点离线，日志中出现 heartbeat timeout。

**解决：**

```yaml
snail-ai:
  client:
    heartbeat-interval: 30s
    reconnect-interval: 5s
    max-reconnect-attempts: -1
```

同时检查 Client 到 Server 的网络延迟和防火墙策略。

### 注册失败

**现象：** Client 启动后未在管理后台应用节点列表中出现。

**排查步骤：**

1. 检查 `app-id` 和 `token` 是否与管理后台创建的应用一致。
2. 确认应用状态为启用。
3. 确认 Server gRPC 端口可访问。
4. 查看 Client 启动日志中的认证或注册错误。

## 模型调用问题

### API Key 无效

**现象：** 调用模型时报 `401 Unauthorized` 或 `Invalid API Key`。

**排查步骤：**

1. 在管理后台模型管理中检查 API Key。
2. 确认 API Key 未过期或被撤销。
3. 如果使用兼容端点，确认 Base URL 和模型名称正确。
4. 检查 `SNAIL_AI_CRYPTO_KEY` 和 `SNAIL_AI_CRYPTO_IV` 是否与录入密钥时一致。

更换加密密钥后，之前存储的 API Key 可能无法解密，需要重新录入。

### 模型调用超时

**现象：** 对话长时间无响应，最终报超时。

**解决：**

1. 检查 Server 或 Client 到模型 API 地址的网络连通性。
2. 增大模型请求超时时间。
3. 检查模型服务本身是否限流或负载过高。

```bash
curl -v https://api.example.com/v1/models -H "Authorization: Bearer $API_KEY"
```

### 速率限制

**现象：** 频繁对话后报 `429 Too Many Requests` 或 `Rate limit exceeded`。

**解决：**

- 降低并发对话数量。
- 在模型提供商控制台提升速率限制。
- 配置更高配额的 API Key 或内网兼容模型服务。

## RAG 知识库问题

### 文档解析失败

**现象：** 上传文档后状态停留在解析中或显示解析失败。

**排查步骤：**

1. 确认文件格式在当前解析链路支持范围内。
2. 确认文件未超过上传限制。
3. 文本类文件建议使用 UTF-8 编码。
4. 查看后端日志中资源、文档解析或 RAG 相关错误。

常见原因包括 PDF 为扫描图片、Office 文档损坏、文件加密或内容过大。

### 向量嵌入失败

**现象：** 文档分片成功但向量化失败。

**排查步骤：**

1. 确认已配置 Embedding 模型。
2. 确认模型 API Key 可用。
3. 检查向量库连接。
4. 检查向量维度是否匹配。

### 检索结果不准确

**优化建议：**

1. 调整分片大小、重叠长度和分片策略。
2. 选择更适合语料的 Embedding 模型。
3. 启用 BM25 或混合检索能力。
4. 配置 Rerank 模型进行重排。
5. 清理文档中的噪声内容。

## 性能问题

### 对话响应缓慢

**排查步骤：**

1. 查看模型调用耗时。
2. 查看 RAG 检索耗时。
3. 检查外部或内网模型服务网络延迟。
4. 检查 Server 和 Client 的 CPU、内存和线程状态。

### 内存溢出

**现象：** 服务崩溃，日志中出现 `OutOfMemoryError`。

**解决：**

```bash
JAVA_OPTS="-Xms2g -Xmx4g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"
```

建议用 MAT 或 VisualVM 分析 Heap Dump，并检查是否存在超大文档处理、批量向量化或并发过高问题。

### GC 停顿过长

**解决：**

```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=50m"
```

### 数据库慢查询

**排查示例：**

```sql
-- MySQL
SHOW PROCESSLIST;

-- PostgreSQL
SELECT pid, now() - pg_stat_activity.query_start AS duration, query, state
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

优化方向包括补充索引、更新统计信息、减少大范围查询和拆分超大表。

## 通用排查工具

### 日志查看

```bash
# 本地或 systemd 环境
journalctl -u snail-ai-server -f --since "10 minutes ago"

# Docker 依赖组件
docker compose -f docs/docker/docker-compose.yaml logs -f elasticsearch
docker compose -f docs/docker/docker-compose.yaml logs -f minio
docker compose -f docs/docker/docker-compose.yaml logs -f milvus
docker compose -f docs/docker/docker-compose.yaml logs -f pgvector
```

### 健康检查

```bash
curl http://localhost:8900/snail-ai/actuator/health
curl http://localhost:8900/snail-ai/actuator/health/db
ss -tlnp | grep -E "8900|18888|3306|5432"
```

### 系统资源监控

```bash
top -p $(pgrep -f snail-ai)
df -h
jps -lv
jstat -gc $(pgrep -f snail-ai) 1000
```

## 获取帮助

提交 Issue 时建议附上：

- Snail AI 版本号。
- 操作系统和 Java 版本。
- 数据库类型和版本。
- 完整错误日志，注意脱敏。
- 复现步骤。

## 下一步

- [配置参考](/deploy/configuration)
- [生产环境部署](/deploy/production)
- [常见问题](/faq/)
