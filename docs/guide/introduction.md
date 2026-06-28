# 项目介绍

## 什么是 Snail AI？

Snail AI 是一款开源 AI Agent 平台，基于 Java 21、Spring Boot 4.1.0 和 Spring AI 2.0 构建，采用 Server-Agent 分布式架构，为企业提供智能体创建、对话、RAG、MCP、Skill、OpenAPI 集成和可观测性能力。

## 概览

默认服务地址：

```text
http://localhost:8900/snail-ai
```

默认 gRPC 端口：

```text
18888
```

## 当前支持状态

| 能力 | 状态 | 对应源码或脚本 |
|------|------|----------------|
| 智能体管理 | 已支持 | `snail-ai-server/snail-ai-server-admin` |
| Server-Agent gRPC 架构 | 已支持 | `snail-ai-agent`、`snail-ai-server` |
| OpenAPI 外部集成 | 已支持 | `snail-ai-server/snail-ai-server-openapi` |
| OpenAI-compatible Chat | 已支持 | `snail-ai-models` |
| OpenAI-compatible Embedding | 已支持 | `snail-ai-models` |
| Qwen/HTTP Rerank | 已支持 | `snail-ai-models` |
| MySQL / PostgreSQL | 已支持 | `docs/sql/` |
| PgVector / Milvus / Elasticsearch | 已支持 | `docs/docker/docker-compose.yaml` |
| MinIO / 本地资源存储 | 已支持 | `snail-ai.resource.*` |
| SQL Server / 达梦 / MariaDB | 规划或适配方向 | 不建议按当前版本直接部署 |

## 设计理念

| 理念 | 说明 |
|------|------|
| 企业级 | 面向生产环境，关注认证、密钥加密、存储、可观测性和部署运维 |
| 可扩展 | 通过责任链、Agent Client、MCP 和 Skill 扩展智能体能力 |
| 自主可控 | 模型调用和工具执行可下发到 Agent Client，便于接入内网资源和本地工具 |
| 源码优先 | 文档描述以当前源码、`pom.xml`、`application.yml` 和 `script/` 为准 |

## 核心优势

- **Spring 生态集成**：基于 Spring Boot 和 Spring AI，适合 Java/Spring 技术栈团队。
- **分布式 Agent 架构**：Server 负责编排，Agent Client 负责模型调用、工具执行和扩展能力。
- **OpenAPI 外部集成**：通过 `Snail-Ai-App-Id` 和 `Snail-Ai-Token` 对外提供应用级接口。
- **RAG 知识库**：支持文档上传、解析、分片、向量化、检索和重排链路。
- **MCP 与 Skill**：支持接入外部 MCP 工具和自定义技能。
- **可观测性**：提供调用链路追踪、耗时分析和评分等能力。
- **多存储选择**：关系库支持 MySQL/PostgreSQL，检索存储支持 PgVector、Milvus、Elasticsearch，资源存储支持本地或 MinIO。

## 技术栈

### 后端

| 技术 | 当前版本或说明 |
|------|----------------|
| Java | 21+ |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0 |
| gRPC | Server-Agent 双向流通信 |
| MyBatis-Plus | ORM 框架 |
| Sa-Token | Admin 认证鉴权 |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3 | 管理端前端框架 |
| TypeScript | 类型安全 |
| Vite | 构建工具 |
| Naive UI | 组件库 |

### 存储

| 类型 | 当前支持 |
|------|----------|
| 关系型数据库 | MySQL、PostgreSQL |
| 向量存储 | PgVector、Milvus |
| 搜索引擎 | Elasticsearch |
| 文件资源 | 本地文件系统、MinIO |

### 模型能力

| 能力 | 当前支持方式 |
|------|--------------|
| Chat | OpenAI-compatible Chat |
| Embedding | OpenAI-compatible Embedding |
| Rerank | Qwen/HTTP Rerank |

Claude、Gemini、Ollama、火山引擎等服务如果提供 OpenAI-compatible 接口，可以按兼容端点方式验证接入；不要理解为当前源码内置的一等 Provider。

## 快速示例

启动依赖组件：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d
```

初始化 MySQL：

```bash
mysql -u root -p snail_ai < docs/sql/snail_ai_schema.sql
```

启动 Server：

```bash
mvn spring-boot:run -pl snail-ai-starter
```

访问管理端：

```text
http://localhost:8900/snail-ai
```

OpenAPI 认证头：

```http
Snail-Ai-App-Id: <your-app-id>
Snail-Ai-Token: <your-app-token>
```

## 开源协议

Snail AI 采用 Apache License 2.0 开源协议，可免费用于商业项目。

## 相关源码

- `pom.xml`
- `snail-ai-starter/src/main/resources/application.yml`
- `snail-ai-server/`
- `snail-ai-agent/`
- `snail-ai-models/`
- `docs/docker/docker-compose.yaml`
- `docs/sql/snail_ai_schema.sql`
- `docs/sql/snail_ai_schema_pgsql.sql`

## 下一步

- [快速开始](/guide/quick-start)
- [系统架构总览](/architecture/overview)
- [配置参考](/deploy/configuration)
- [OpenAPI](/api/openapi/)
