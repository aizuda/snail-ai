# 生产环境部署

本文介绍 Snail AI 0.0.6 的生产环境部署建议，内容以当前源码、`application.yml` 和 `script/` 目录为准。

## 概览

生产部署需要关注 Server 资源规划、JVM 参数、反向代理、HTTPS、数据库、向量检索存储、Agent Client 节点和备份策略。当前默认 HTTP 端口为 `8900`，上下文路径为 `/snail-ai`，gRPC 端口为 `18888`。

## 当前支持状态

| 能力 | 状态 | 对应源码或脚本 |
|------|------|----------------|
| Server HTTP 服务 | 已支持，默认 `8900` + `/snail-ai` | `snail-ai-starter/src/main/resources/application.yml` |
| Server gRPC | 已支持，默认 `18888` | `snail-ai-starter/src/main/resources/application.yml` |
| MySQL | 已支持 | `docs/sql/snail_ai_schema.sql` |
| PostgreSQL | 已支持 | `docs/sql/snail_ai_schema_pgsql.sql` |
| PgVector | 已支持 | `docs/docker/docker-compose.yaml` |
| Milvus | 已支持 | `docs/docker/docker-compose.yaml` |
| Elasticsearch | 已支持 | `docs/docker/docker-compose.yaml` |
| MinIO / 本地资源存储 | 已支持 | `snail-ai.resource.*` |
| SQL Server / 达梦 / MariaDB | 规划或适配方向 | 不建议按当前版本直接部署 |

## 硬件资源推荐

### Snail AI Server

| 规模 | CPU | 内存 | 磁盘 | 适用场景 |
|------|-----|------|------|----------|
| 小型试用 | 2 核 | 4 GB | 50 GB SSD | 10 人以内体验评估 |
| 团队使用 | 4 核 | 8 GB | 100 GB SSD | 50 人以内日常使用 |
| 企业生产 | 8 核+ | 16 GB+ | 500 GB+ SSD | 100+ 用户或高并发场景 |

### 数据库与检索组件

| 组件 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| MySQL / PostgreSQL | 2-4 核 | 4-8 GB | 100 GB+ SSD | 推荐 SSD 以保证 IO 性能 |
| PgVector | 2-4 核 | 4-8 GB | 按向量规模计算 | 可与 PostgreSQL 共用实例 |
| Milvus | 4-8 核 | 8-16 GB | 200 GB+ SSD | 适合更大规模向量检索 |
| Elasticsearch | 4-8 核 | 8-16 GB | 200 GB+ SSD | 适合全文检索和混合检索 |
| MinIO | 2-4 核 | 4 GB+ | 按文件规模计算 | 生产环境推荐对象存储 |

### Agent Client 节点

| 场景 | CPU | 内存 | 说明 |
|------|-----|------|------|
| 轻量节点 | 2 核 | 2 GB | 主要负责模型 API 转发和工具调用 |
| 标准节点 | 4 核 | 4 GB | 适合常规工具、MCP、网络请求等任务 |
| 重量级节点 | 8 核+ | 16 GB+ | 适合运行本地工具或内网模型服务 |

## 网络要求

- Browser 到 Server：HTTP/SSE，默认 `http://<host>:8900/snail-ai`。
- Server 到 Client：gRPC，默认 Server 监听 `18888`。
- Server / Client 到模型服务：取决于你配置的 OpenAI-compatible Chat、Embedding 或 Qwen/HTTP Rerank 地址。
- Server 到存储组件：数据库、向量库、Elasticsearch、MinIO 等建议走内网。

## JVM 调优

生产环境可从以下参数开始，再根据实际负载调整：

```bash
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8"
```

| 用户规模 | 堆内存建议 | 说明 |
|----------|------------|------|
| 10 人以内 | `-Xms512m -Xmx1g` | 试用评估 |
| 50 人以内 | `-Xms1g -Xmx2g` | 团队使用 |
| 100+ 用户 | `-Xms2g -Xmx4g` | 企业生产 |
| 高并发 | `-Xms4g -Xmx8g` | 大量并发对话或文档处理 |

建议开启 GC 日志：

```bash
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=50m"
```

## Nginx 反向代理

### HTTP 示例

```nginx
upstream snail_ai_backend {
    server 127.0.0.1:8900;
    keepalive 32;
}

server {
    listen 80;
    server_name ai.example.com;

    location /snail-ai/ {
        proxy_pass http://snail_ai_backend/snail-ai/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        chunked_transfer_encoding on;
    }

    client_max_body_size 100m;
    gzip on;
    gzip_types text/plain application/json application/javascript text/css text/xml;
    gzip_min_length 1024;
    gzip_comp_level 5;

    add_header X-Frame-Options SAMEORIGIN;
    add_header X-Content-Type-Options nosniff;
}
```

### HTTPS 建议

生产环境建议启用 HTTPS，并将 80 端口重定向到 443：

```nginx
server {
    listen 80;
    server_name ai.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ai.example.com;

    ssl_certificate /etc/letsencrypt/live/ai.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ai.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    location /snail-ai/ {
        proxy_pass http://127.0.0.1:8900/snail-ai/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        chunked_transfer_encoding on;
    }

    client_max_body_size 100m;
}
```

## 数据库选型

| 场景 | 推荐数据库 | 理由 |
|------|------------|------|
| 默认部署和快速上线 | MySQL 8.0+ | 当前脚本已提供，生态成熟 |
| 希望关系库兼容 PostgreSQL | PostgreSQL | 当前脚本已提供 |
| 希望关系库和向量库复用 | PostgreSQL + PgVector | 架构简单，便于统一备份 |
| 大规模向量检索 | MySQL/PostgreSQL + Milvus | 向量检索能力更强 |
| 需要全文检索或混合检索 | MySQL/PostgreSQL + Elasticsearch | 支持 BM25 和搜索场景 |

SQL Server、达梦、MariaDB 不应出现在当前版本的生产部署主流程中；如需适配，请先以源码和初始化脚本为准做专项验证。

## 向量数据库容量规划

以 1536 维向量为例，单向量原始存储约 `1536 x 4 = 6144` 字节，索引会带来额外开销。

| 文档数量 | 预估分片数 | 原始向量空间 | 推荐方案 |
|----------|------------|--------------|----------|
| 1,000 篇 | ~10,000 | ~60 MB | PgVector |
| 10,000 篇 | ~100,000 | ~600 MB | PgVector |
| 100,000 篇 | ~1,000,000 | ~6 GB | PgVector / Milvus |
| 1,000,000 篇 | ~10,000,000 | ~60 GB | Milvus |
| 10,000,000+ 篇 | ~100,000,000+ | ~600 GB+ | Milvus 集群 |

| 方案 | 推荐场景 |
|------|----------|
| PgVector | 中小规模、PostgreSQL 技术栈、低运维成本 |
| Milvus | 大规模向量、需要更强向量检索性能 |
| Elasticsearch | 需要全文检索、关键词检索、混合检索 |

## 多节点 Agent Client

多个 Client 节点可以使用同一应用的 `appId` 和 `token` 连接 Server，以提升可用性和吞吐。

```yaml
snail-ai:
  app-id: ${APP_ID}
  token: ${APP_TOKEN}
  server:
    host: ${SERVER_HOST}
    port: 18888
  client:
    name: ${NODE_NAME}
```

建议：

- 同一应用至少部署 2 个 Client 节点。
- Client 到 Server 的 `18888` 端口保持内网可达。
- 节点名称保持唯一，便于后台定位问题。
- 需要访问内网工具或模型服务时，把对应 Client 部署在同一网络域内。

## systemd 部署示例

```ini
[Unit]
Description=Snail AI Server
After=network.target

[Service]
Type=simple
User=snailai
Group=snailai
WorkingDirectory=/opt/snail-ai
Environment="JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
EnvironmentFile=/opt/snail-ai/.env
ExecStart=/usr/bin/java $JAVA_OPTS -jar snail-ai-starter.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

常用命令：

```bash
systemctl daemon-reload
systemctl start snail-ai-server
systemctl enable snail-ai-server
systemctl status snail-ai-server
journalctl -u snail-ai-server -f
```

## 生产环境检查清单

- [ ] 修改 Admin 默认密码、数据库默认密码和 MinIO 默认密码。
- [ ] 通过环境变量配置 `SNAIL_AI_CRYPTO_KEY` 和 `SNAIL_AI_CRYPTO_IV`，并妥善备份。
- [ ] 启用 HTTPS。
- [ ] 根据负载调整 JVM 堆内存和 GC 参数。
- [ ] 配置数据库、MinIO、向量库的定期备份。
- [ ] 配置日志轮转，避免磁盘写满。
- [ ] 配置 CPU、内存、磁盘、数据库连接数和模型调用错误率告警。
- [ ] 防火墙仅开放必要端口：80/443 对外，`8900` 和 `18888` 建议仅内网访问。
- [ ] 生产环境优先使用 MinIO 或对象存储，不建议多节点共享本地上传目录。
- [ ] 根据文档规模选择 PgVector、Milvus 或 Elasticsearch。

## 相关源码

- `snail-ai-starter/src/main/resources/application.yml`
- `docs/docker/docker-compose.yaml`
- `docs/sql/snail_ai_schema.sql`
- `docs/sql/snail_ai_schema_pgsql.sql`
- `pom.xml`

## 下一步

- [配置参考](/deploy/configuration)
- [Docker 部署](/deploy/docker)
- [故障排除](/deploy/troubleshooting)
