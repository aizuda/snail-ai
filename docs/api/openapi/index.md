# OpenAPI 概述

OpenAPI 是 Snail AI 面向第三方系统和自定义前端提供的外部集成接口。当前版本以应用凭证认证，接口前缀为 `/snail-ai/openapi/v1`。

## 当前支持状态

| 能力 | 状态 | 对应源码 |
|------|------|----------|
| 应用级认证 | 已支持，使用 `Snail-Ai-App-Id` + `Snail-Ai-Token` | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/interceptor/OpenApiAuthInterceptor.java` |
| 智能体查询与订阅 | 已支持 | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/OpenApiAgentController.java` |
| 用户注册与查询 | 已支持 | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/OpenApiUserController.java` |
| 会话管理与消息查询 | 已支持 | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/OpenApiConversationController.java` |
| 智能体流式/同步对话 | 已支持 | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/OpenApiChatController.java` |
| 客户端 Chat 会话 Token | 已支持 | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/OpenApiEmbedController.java` |

## Base URL

```text
{protocol}://{host}:{port}/snail-ai/openapi/v1
```

本地默认示例：

```text
http://localhost:8900/snail-ai/openapi/v1
```

## 认证方式

OpenAPI 外部集成请求必须携带应用 ID 和应用 Token：

```http
Snail-Ai-App-Id: <your-app-id>
Snail-Ai-Token: <your-app-token>
```

应用 ID 和 Token 在管理后台的应用管理中创建和复制。`Snail-Ai-Auth` 是 Admin API 与客户端 Chat 会话使用的认证头，不是 OpenAPI 外部集成的通用认证头。

详见 [认证方式](./auth.md)。

## 可用接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 智能体列表 | `GET /agents` | 查询应用可访问的智能体 |
| 智能体详情 | `GET /agent` | 查询单个智能体详情 |
| 用户注册 | `POST /user/register` | 注册或同步外部用户 |
| 当前用户 | `GET /user` | 查询外部用户信息 |
| 用户智能体列表 | `GET /user/agents` | 查询外部用户订阅的智能体 |
| 订阅/取消订阅智能体 | `POST/DELETE /user/agent` | 管理外部用户与智能体关系 |
| 会话列表 | `GET /agent/conversations` | 查询智能体会话 |
| 创建会话 | `POST /agent/conversations` | 创建智能体会话 |
| 删除/清空会话 | `DELETE /agent/conversations` | 删除指定会话或清空智能体会话 |
| 会话消息 | `GET /agent/conversations/messages` | 查询会话消息 |
| 流式对话 | `POST /agent/chat` | SSE 流式对话 |
| 同步对话 | `POST /agent/chat/sync` | 同步返回完整回答 |
| Chat 会话 Token | `GET/POST /embed-token` | 生成客户端 Chat 会话 Token |

## 快速体验

```bash
curl -N -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>' \
  -d '{
    "agentId": 1,
    "openId": "external-user-001",
    "content": "你好，请介绍一下你自己"
  }'
```

## 通用响应格式

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

## 与 Admin API 的区别

| 特性 | Admin API | OpenAPI |
|------|-----------|---------|
| 面向对象 | 管理后台用户 | 第三方系统/外部用户 |
| 路径前缀 | `/snail-ai/` | `/snail-ai/openapi/v1/` |
| 认证方式 | `Snail-Ai-Auth` | `Snail-Ai-App-Id` + `Snail-Ai-Token` |
| 用户标识 | 平台用户 | 外部 `openId` |

## 相关源码

- `snail-ai-commons/snail-ai-commons-core/src/main/java/com/aizuda/snail/ai/common/constants/OpenApiPathConstants.java`
- `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/`
- `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/interceptor/OpenApiAuthInterceptor.java`
