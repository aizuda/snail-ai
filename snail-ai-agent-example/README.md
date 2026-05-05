# Snail AI Agent Example

OpenAPI 客户端使用示例项目，演示如何通过 OpenAPI Client 调用 Snail AI 服务端接口。

## 功能特性

- **Agent 管理**：查询 Agent 列表和详情
- **会话管理**：创建、查询、删除会话，查看消息历史
- **同步对话**：发送消息并等待完整回复
- **流式对话**：通过 SSE 实时接收 AI 响应流

## 快速开始

### 1. 启动服务端

确保 Snail AI 主服务运行在默认端口：

```bash
# 启动 snail-ai-admin 服务（默认端口 17888）
cd snail-ai-admin
mvn spring-boot:run
```

### 2. 配置客户端

编辑 `src/main/resources/application.yml`：

```yaml
snail-ai:
  openapi-client:
    server-host: localhost  # 服务端主机
    server-port: 17888      # 服务端端口
    connect-timeout: 30000  # 连接超时（毫秒）
    read-timeout: 300000    # 读取超时（毫秒）
```

### 3. 启动示例应用

```bash
cd snail-ai-agent-example
mvn spring-boot:run
```

应用将启动在 `http://localhost:17889`

### 4. 访问 Swagger UI

打开浏览器访问：

```
http://localhost:17889/swagger-ui.html
```

## API 接口说明

### Agent 相关

- `GET /demo/agents` - 获取所有 Agent 列表
- `GET /demo/agent/{agentId}` - 获取 Agent 详情

### 会话管理

- `POST /demo/agent/{agentId}/conversation` - 创建会话
- `GET /demo/agent/{agentId}/conversations` - 获取会话列表（分页）
- `GET /demo/agent/{agentId}/conversation/{conversationId}/messages` - 获取消息历史
- `DELETE /demo/agent/{agentId}/conversation/{conversationId}` - 删除会话

### 对话接口

- `POST /demo/agent/{agentId}/chat/sync` - 同步对话
- `GET /demo/agent/{agentId}/chat/stream` - 流式对话（SSE）

## 使用示例

### 1. 获取 Agent 列表

```bash
curl -X GET "http://localhost:17889/demo/agents" \
  -H "App-Id: your-app-id" \
  -H "Token: your-token"
```

### 2. 创建会话

```bash
curl -X POST "http://localhost:17889/demo/agent/1/conversation" \
  -H "App-Id: your-app-id" \
  -H "Token: your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123"
  }'
```

### 3. 同步对话

```bash
curl -X POST "http://localhost:17889/demo/agent/1/chat/sync" \
  -H "App-Id: your-app-id" \
  -H "Token: your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-123",
    "content": "你好，介绍一下自己"
  }'
```

### 4. 流式对话

```bash
curl -X GET "http://localhost:17889/demo/agent/1/chat/stream?content=你好" \
  -H "App-Id: your-app-id" \
  -H "Token: your-token" \
  --no-buffer
```

或在浏览器中使用 EventSource：

```javascript
const eventSource = new EventSource(
  'http://localhost:17889/demo/agent/1/chat/stream?content=你好'
);

eventSource.addEventListener('text', (e) => {
  console.log('AI回复:', e.data);
});

eventSource.addEventListener('thinking', (e) => {
  console.log('AI思考:', e.data);
});

eventSource.addEventListener('done', (e) => {
  console.log('对话完成:', e.data);
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  console.error('错误:', e.data);
  eventSource.close();
});
```

## 注意事项

1. **认证信息**：所有请求都需要在 Header 中携带 `App-Id` 和 `Token`
2. **超时配置**：流式对话建议设置较长的超时时间（5 分钟）
3. **会话 ID**：conversationId 格式为字符串（如 "conv-123"），非数字 ID
4. **SSE 支持**：流式接口返回 SSE 事件流，需要客户端支持 Server-Sent Events

## 技术栈

- Spring Boot 3.4.1
- Snail AI OpenAPI Client
- Springdoc OpenAPI (Swagger UI)
- Lombok

## 项目结构

```
snail-ai-agent-example/
├── src/main/java/
│   └── com/aizuda/snail/ai/agent/example/
│       ├── SnailAiAgentExampleApplication.java  # 启动类
│       ├── config/
│       │   └── SwaggerConfig.java                # Swagger 配置
│       └── controller/
│           └── OpenApiDemoController.java        # 示例 Controller
└── src/main/resources/
    └── application.yml                           # 配置文件
```

## 开发建议

1. 使用 Swagger UI 进行接口测试，无需编写测试代码
2. 参考 `OpenApiDemoController` 中的示例代码集成到自己的项目
3. 流式对话建议使用异步处理，避免阻塞主线程
4. 生产环境建议添加完善的异常处理和日志记录

## 常见问题

**Q: 编译失败怎么办？**

A: 确保先编译父项目：
```bash
cd snail-ai
mvn clean install -DskipTests
```

**Q: 连接服务端失败？**

A: 检查以下几点：
- 服务端是否正常启动
- `application.yml` 中的 `server-host` 和 `server-port` 是否正确
- 防火墙是否允许访问

**Q: Swagger UI 打不开？**

A: 确认端口 17889 没有被占用，可修改 `application.yml` 中的 `server.port`

## 更多资源

- [Snail AI 官方文档](https://github.com/opensnail/snail-ai)
- [OpenAPI 规范](https://swagger.io/specification/)
- [SSE 标准](https://html.spec.whatwg.org/multipage/server-sent-events.html)
