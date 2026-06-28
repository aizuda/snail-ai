# 认证方式

OpenAPI 外部集成使用应用级凭证认证。每个请求都必须携带应用 ID 和应用 Token。

## 当前支持状态

| 认证场景 | Header | 说明 | 对应源码 |
|----------|--------|------|----------|
| OpenAPI 外部集成 | `Snail-Ai-App-Id`、`Snail-Ai-Token` | 第三方系统调用 `/snail-ai/openapi/v1/**` | `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/interceptor/OpenApiAuthInterceptor.java` |
| Admin API | `Snail-Ai-Auth` | 管理后台登录态 | `snail-ai-server/snail-ai-server-admin` |
| 智能体对话 | `Snail-Ai-Auth` | `/snail-chat` 页面与 `/api/snail/chat/**` 网关会话 Token | `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/java/com/aizuda/snail/ai/agent/chat/starter/SnailAiChatTokenService.java` |

## 获取应用凭证

1. 登录 Snail AI 管理后台。
2. 进入「应用管理」。
3. 创建应用或打开已有应用。
4. 复制应用的 `appId` 和 `token`。

应用 Token 适合服务端集成场景，请不要硬编码到公开前端代码中。

## 请求认证

```http
Snail-Ai-App-Id: <your-app-id>
Snail-Ai-Token: <your-app-token>
```

示例：

```bash
curl -X GET 'http://localhost:8900/snail-ai/openapi/v1/agents?page=1&size=10' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>'
```

## 对话请求示例

```bash
curl -N -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>' \
  -d '{
    "agentId": 1,
    "openId": "external-user-001",
    "content": "你好"
  }'
```

## 常见错误

| 问题 | 说明 | 处理方式 |
|------|------|----------|
| 缺少认证信息 | 未提供 `Snail-Ai-App-Id` 或 `Snail-Ai-Token` | 同时传入两个 Header |
| Token 校验失败 | Token 与 App 不匹配或已变更 | 到应用管理重新复制 Token |
| 使用了 `Snail-Ai-Auth` | 将 Admin/智能体对话认证头误用于 OpenAPI | 改用 `Snail-Ai-App-Id` + `Snail-Ai-Token` |

## 最佳实践

1. 服务端通过环境变量或密钥管理系统保存 App Token。
2. 每个外部系统使用独立 App，便于审计和停用。
3. Token 泄露后立即在管理后台重新生成或禁用对应应用。
4. 前端直连时应通过自己的后端代理转发，避免公开 App Token。
