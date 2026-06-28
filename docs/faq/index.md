# 常见问题

本页整理 Snail AI 0.0.6 使用过程中的常见问题。回答以当前源码、`pom.xml`、`application.yml` 和 `script/` 目录为准。

## 安装与环境

### Q：Snail AI 需要什么版本的 Java？

Snail AI 0.0.6 要求 **Java 21** 或更高版本。根 `pom.xml` 中的 `java.version` 为 `21`。

```bash
java -version
```

### Q：默认访问地址是什么？

默认 HTTP 端口是 `8900`，上下文路径是 `/snail-ai`：

```text
http://localhost:8900/snail-ai
```

gRPC 默认端口是 `18888`。

### Q：如何选择关系型数据库？

| 场景 | 推荐数据库 | 状态 |
|------|------------|------|
| 默认体验和常规部署 | MySQL 8.0+ | 已支持 |
| 希望业务库兼容 PostgreSQL | PostgreSQL | 已支持，脚本为 `docs/sql/snail_ai_schema_pgsql.sql` |
| 希望关系库和向量库复用 PostgreSQL | PostgreSQL + PgVector | 已支持，需安装 pgvector 扩展 |
| SQL Server / 达梦 / MariaDB | 仅作为规划或适配方向 | 不建议按当前文档直接部署 |

### Q：如何选择向量或检索存储？

| 场景 | 推荐方案 | 状态 |
|------|----------|------|
| 轻量部署、PostgreSQL 技术栈 | PgVector | 已支持 |
| 大规模向量检索 | Milvus | 已支持 |
| 需要全文检索和混合检索 | Elasticsearch | 已支持 |

### Q：是否可以不部署向量数据库？

可以。如果不使用 RAG 知识库，可以先不配置向量库。使用 RAG 文档检索、向量搜索或重排链路时，需要配置 PgVector、Milvus 或 Elasticsearch 等存储。

## 智能体与模型

### Q：智能体支持哪些模型？

当前源码已落地的调用规格是：

| 能力 | 支持方式 |
|------|----------|
| Chat | OpenAI-compatible Chat |
| Embedding | OpenAI-compatible Embedding |
| Rerank | Qwen/HTTP Rerank |

Claude、Gemini、Ollama、火山引擎等服务如果提供 OpenAI-compatible 接口，可以按兼容端点方式验证接入；不要把它们理解为当前源码内置的一等 Provider。

### Q：一个智能体可以绑定多少个 MCP 工具？

一个智能体可以绑定多个 MCP 服务。实际数量受模型上下文窗口影响，因为工具名称、描述和参数会占用上下文。建议先控制在 20 个以内，再根据模型效果调整。

### Q：如何让智能体使用知识库回答？

1. 创建知识库并上传文档。
2. 等待文档解析、分片和向量化完成。
3. 在智能体配置中绑定知识库。
4. 通过管理端对话或 OpenAPI `/openapi/v1/agent/chat` 调用该智能体。

## RAG 知识库

### Q：支持哪些文档格式？

当前文档解析链路覆盖常见办公和文本格式，包括 PDF、Word、Excel、HTML、Markdown 和纯文本等。具体可用格式以资源上传和解析器源码为准。

### Q：检索结果不准确怎么办？

1. 调整分片大小和分片策略。
2. 使用更适合语料的 Embedding 模型。
3. 启用 BM25/混合检索能力。
4. 配置 Rerank 模型进行重排。
5. 清理文档中的噪声内容。

## 客户端与 OpenAPI

### Q：Client 节点如何接入 Server？

Client 通过 gRPC 与 Server 通信，默认端口为 `18888`。应用管理中生成的 `appId` 和 `token` 用于节点认证。

### Q：OpenAPI 使用什么认证头？

OpenAPI 外部集成使用：

```http
Snail-Ai-App-Id: <your-app-id>
Snail-Ai-Token: <your-app-token>
```

`Snail-Ai-Auth` 是 Admin API 和嵌入式聊天会话使用的认证头，不是 OpenAPI 外部集成的通用认证头。

## 部署运维

### Q：Docker Compose 文件在哪里？

当前 Docker Compose 文件位于：

```text
docs/docker/docker-compose.yaml
```

启动依赖组件：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d
```

当前 Compose 文件主要启动 Elasticsearch、MinIO、Milvus 和 PgVector 等依赖组件，不直接包含 Snail AI Server 容器。

### Q：SQL 初始化脚本在哪里？

| 数据库 | 脚本 |
|--------|------|
| MySQL | `docs/sql/snail_ai_schema.sql` |
| PostgreSQL | `docs/sql/snail_ai_schema_pgsql.sql` |

### Q：如何备份数据？

需要备份：

1. 关系型数据库：`mysqldump` 或 `pg_dump`。
2. 向量存储：PgVector 随 PostgreSQL 备份，Milvus 使用官方备份工具。
3. 文件资源：本地上传目录或 MinIO Bucket。
4. 配置和密钥：`application.yml`、环境变量和加密密钥。

## 模型与费用

### Q：使用 Snail AI 需要付费吗？

Snail AI 本身采用 Apache 2.0 协议开源。大模型 API、云数据库、对象存储等外部服务费用由你选择的服务商决定。

### Q：可以离线私有化部署吗？

Snail AI 可以私有化部署。是否完全离线取决于你选择的模型服务和存储组件；如果模型服务在内网提供 OpenAI-compatible 接口，则业务调用可不出内网。

## 相关源码

- `pom.xml`
- `snail-ai-starter/src/main/resources/application.yml`
- `docs/docker/docker-compose.yaml`
- `docs/sql/snail_ai_schema.sql`
- `docs/sql/snail_ai_schema_pgsql.sql`
