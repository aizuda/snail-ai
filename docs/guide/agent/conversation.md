# 智能体对话

## 概览

智能体对话页面由独立前端项目和 Agent Chat 后端模块共同组成，用于提供可独立访问或嵌入业务系统的聊天界面。

当前源码结构：

| 部分 | 位置 | 说明 |
|------|------|------|
| 对话前端源码 | `snail-ai-chat` | 独立 Vue 3 前端项目，默认网关路径为 `/api/snail/chat` |
| Chat 后端模块 | `snail-ai-agent/snail-ai-agent-chat` | Agent 侧 Chat API 与 Starter |
| 后端 API 包 | `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-api` | 会话、认证注解、配置和凭证校验扩展点 |
| 后端 Starter | `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter` | 自动装配、静态资源、网关 Controller、Token 服务和认证拦截器 |
| 前端构建产物 | `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/resources/META-INF/chat` | `snail-ai-chat` 构建后复制到 Starter 的静态资源目录 |

## 当前支持状态

| 能力 | 状态 | 对应源码 |
|------|------|----------|
| Chat 页面入口 | 已支持 | `WebController` 映射 `/snail-chat` |
| Chat 网关接口 | 已支持 | `SnailAiChatGatewayController` 映射 `/api/snail/chat` |
| 会话 Token | 已支持 | `SnailAiChatTokenService`，Header 为 `Snail-Ai-Auth` |
| 会话认证 | 已支持 | `SnailAiChatAuthenticationInterceptor` + `@SnailAiChatAuthorize` |
| 可信凭据校验扩展 | 已支持 | `SnailAiChatCredentialValidator` |
| 前端运行时配置 | 已支持 | `snail-ai.chat.ui.*` 与 `/api/snail/chat/config` |
| 嵌入模式 UI 配置 | 已支持 | `snail-ai.chat.ui.embed.*` |

`SnailAiChatAutoConfiguration` 依赖 OpenAPI 客户端能力，当前由 `snail-ai.openapi.enabled=true` 触发自动装配。

## 访问路径

| 类型 | 路径 | 说明 |
|------|------|------|
| 页面入口 | `/snail-chat` | 重定向到 `/snail-chat/` |
| 静态资源 | `/snail-chat/**` | 从 `classpath:/META-INF/chat/` 读取 |
| Chat 网关 | `/api/snail/chat/**` | 前端访问后端的统一网关 |

前端默认从 `/api/snail/chat/config` 读取后端配置，再使用同一网关路径访问会话、智能体、会话列表和流式对话接口。

## 本地访问

后端 Chat Starter 本地启动在 `8081` 端口时，可以直接访问：

```text
http://localhost:8081/snail-chat
```

初始化 SQL 内置的本地体验 `openId` 为：

```text
46ed53c6a20044c7bbd870848e80f92f
```

如需显式传入 `openId`，也可以使用：

```text
http://localhost:8081/snail-chat?openId=46ed53c6a20044c7bbd870848e80f92f
```

## 界面预览

首次进入页面时，输入 `openId` 后即可创建智能体对话会话。

![智能体对话 openId 输入页](/images/client-chat/openid-page.png)

会话创建成功后进入对话页面，可选择智能体、查看会话列表并进行流式对话。

![智能体对话页面](/images/client-chat/conversation-page.png)

## 网关接口

| 接口 | 方法与路径 | 说明 |
|------|------------|------|
| 运行时配置 | `GET /api/snail/chat/config` | 返回页面标题、Logo、资源地址和嵌入模式配置 |
| 创建会话 | `POST /api/snail/chat/session` | 根据 `openId` 和可选 `trustedCredential` 创建 Chat 会话 Token |
| 智能体列表 | `GET /api/snail/chat/agents` | 查询可访问智能体，并标记订阅状态 |
| 智能体详情 | `GET /api/snail/chat/agent` | 查询单个智能体 |
| 我的智能体 | `GET /api/snail/chat/my-agents` | 查询当前外部用户订阅的智能体 |
| 订阅智能体 | `POST /api/snail/chat/agent/subscribe` | 订阅智能体 |
| 取消订阅 | `DELETE /api/snail/chat/agent/subscribe` | 取消订阅智能体 |
| 会话列表 | `GET /api/snail/chat/conversations` | 查询智能体会话 |
| 创建会话 | `POST /api/snail/chat/conversations` | 创建智能体会话 |
| 删除/清空会话 | `DELETE /api/snail/chat/conversations` | 删除指定会话或清空智能体会话 |
| 会话消息 | `GET /api/snail/chat/messages` | 查询会话消息 |
| 流式对话 | `POST /api/snail/chat/completions` | SSE 流式对话 |

除 `/config` 与 `/session` 外，网关接口通过 `Snail-Ai-Auth` 携带会话 Token。该 Header 只用于 Admin API 或智能体对话会话，不是 OpenAPI 外部集成的应用凭证。

## 配置示例

```yaml
snail-ai:
  openapi:
    enabled: true
  chat:
    ui:
      page-title: Snail AI Chat
      logo: https://snailjob.opensnail.com/logo.svg
      resource-base-url:
      embed:
        enabled: false
        show-header: false
        show-sidebar-user: false
        show-agent-market: false
        compact-input: true
        lock-agent: false
    session:
      token-ttl-seconds: 3600
```

前端 URL 参数可临时覆盖部分嵌入模式配置，例如：

```text
/snail-chat/?openId=user-001&agentId=1&embed=1&showHeader=0&lockAgent=1
```

## 前端构建与嵌入

`snail-ai-chat` 的 `pnpm build` 会执行类型检查、Vite 构建，并通过 `scripts/embed.mjs` 将 `dist` 复制到 Agent Chat Starter：

```text
snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/resources/META-INF/chat
```

如果只需要生成前端 `dist`，使用 `pnpm build:only`；如果只需要把已有 `dist` 复制进后端资源目录，使用 `pnpm embed`。

## 相关源码

- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-api/src/main/java/com/aizuda/snail/ai/agent/chat/api/SnailAiChatProperties.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-api/src/main/java/com/aizuda/snail/ai/agent/chat/api/SnailAiChatAuthorize.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-api/src/main/java/com/aizuda/snail/ai/agent/chat/api/SnailAiChatCredentialValidator.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/java/com/aizuda/snail/ai/agent/chat/starter/SnailAiChatAutoConfiguration.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/java/com/aizuda/snail/ai/agent/chat/starter/SnailAiChatGatewayController.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/java/com/aizuda/snail/ai/agent/chat/starter/SnailAiChatAuthenticationInterceptor.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/java/com/aizuda/snail/ai/agent/chat/starter/SnailAiChatTokenService.java`
- `snail-ai-agent/snail-ai-agent-chat/snail-ai-agent-chat-starter/src/main/java/com/aizuda/snail/ai/agent/chat/starter/WebController.java`
