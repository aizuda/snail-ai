# 快速开始

## 在线体验

不想本地部署？直接访问体验版管理后台：

- 地址：[https://snailai-admin.opensnail.com/](https://snailai-admin.opensnail.com/)
- 账号：`admin` / `admin123`

体验版已预置示例模型和智能体，登录后即可体验对话、RAG、MCP 等核心功能。详见 [在线体验](/guide/preview)。

## 本地部署

本指南用于在本地启动 Snail AI 1.1.1。当前仓库包含后端、Agent、OpenAPI、内置管理端静态资源和文档站；Docker 与 SQL 脚本位于根目录 `script/`。

## 当前支持状态

| 项目 | 当前版本/路径 | 来源 |
|------|---------------|------|
| Java | 21+ | `pom.xml` |
| Spring Boot | 4.1.0 | `pom.xml` |
| Spring AI | 2.0.0 | `pom.xml` |
| HTTP 端口 | `8900` | `snail-ai-starter/src/main/resources/application.yml` |
| Context Path | `/snail-ai` | `snail-ai-starter/src/main/resources/application.yml` |
| gRPC 端口 | `18888` | `snail-ai-starter/src/main/resources/application.yml` |
| MySQL 初始化脚本 | `script/sql/snail_ai_schema.sql` | 仓库脚本 |
| PostgreSQL 初始化脚本 | `script/sql/snail_ai_schema_pgsql.sql` | 仓库脚本 |
| 依赖组件 Docker Compose | `docs/docker/docker-compose.yaml` | 仓库脚本 |

## 环境准备

| 软件 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 21+ | 推荐 OpenJDK 21 |
| Maven | 3.8+ | 后端构建工具 |
| MySQL | 8.0+ | 默认业务数据库 |
| PostgreSQL + PgVector | 16+ | 可作为 PostgreSQL 业务库或向量库 |
| Docker Compose | v2+ | 可选，用于启动 PgVector、Milvus、Elasticsearch、MinIO 等依赖 |

## 第一步：克隆仓库

```bash
git clone https://gitee.com/aizuda/snail-ai.git
cd snail-ai
```

## 第二步：启动依赖组件（可选）

如果你需要 PgVector、Milvus、Elasticsearch 或 MinIO，可使用仓库提供的 Compose 文件：

```bash
docker compose -f docs/docker/docker-compose.yaml up -d
```

该 Compose 文件负责启动依赖组件，不包含 Snail AI Server 本身。

## 第三步：初始化数据库

### MySQL

```sql
CREATE DATABASE snail_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bash
mysql -u root -p snail_ai < script/sql/snail_ai_schema.sql
```

### PostgreSQL

```bash
createdb -U postgres snail_ai
psql -U postgres -d snail_ai -f script/sql/snail_ai_schema_pgsql.sql
```

如果同时使用 PgVector，请确保目标 PostgreSQL 实例安装了 `vector` 扩展。

## 第四步：配置后端

编辑：

```text
snail-ai-starter/src/main/resources/application.yml
```

MySQL 示例：

```yaml
server:
  port: 8900
  servlet:
    context-path: /snail-ai

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/snail_ai?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true
    username: root
    password: your_password

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

## 第五步：启动服务

```bash
mvn spring-boot:run -pl snail-ai-starter
```

启动成功后访问：

```text
http://localhost:8900/snail-ai
```

默认管理员账号：

- 用户名：`admin`
- 密码：`admin123`

## 第六步：完成首次对话

1. 登录管理端。
2. 在「模型管理」中创建 OpenAI-compatible Chat 模型配置，点击「验证」确认连通后设置为默认对话模型。
3. 在「应用管理」中创建应用，复制 `appId` 和 `token` 供 Agent Client 或 OpenAPI 使用。
4. 在「智能体管理」中创建智能体。
5. 进入智能体对话页面开始测试。

## 常用验证命令

```bash
curl http://localhost:8900/snail-ai/actuator/health
```

OpenAPI 对话示例（替换应用凭证后可直接复制执行）：

```bash
SNAIL_AI_APP_ID='<your-app-id>'
SNAIL_AI_TOKEN='<your-app-token>'

curl -N -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H "Snail-Ai-App-Id: ${SNAIL_AI_APP_ID}" \
  -H "Snail-Ai-Token: ${SNAIL_AI_TOKEN}" \
  --data-binary @- <<'JSON'
{
  "agentId": 1,
  "openId": "external-user-001",
  "conversationId": "conv-abc123",
  "content": "你好"
}
JSON
```

## 下一步

- [模型管理](/guide/model/)：配置 Chat、Embedding、Rerank 模型。
- [RAG 知识库](/guide/rag/)：创建知识库并绑定智能体。
- [MCP 集成](/guide/mcp/)：接入 SSE、Streamable HTTP 或 Stdio MCP 服务。
- [Docker 部署](/deploy/docker)：了解 `docs/docker/docker-compose.yaml` 中的依赖组件。
