# 客户端日志

当前开源版本的 Agent Client 提供日志拦截、Token 用量采集和思维内容采集能力。这些能力用于开发调试和对话结果持久化，不包含独立的可视化后台。

## LoggingInterceptor

`LoggingInterceptor` 是可选的请求/响应日志拦截器。启用后会记录每次 LLM 请求的消息数量和响应完成原因。

```yaml
snail-ai:
  agent:
    logging-interceptor: true
```

日志示例：

```text
LLM request: 5 messages
LLM response: finishReason=STOP
```

相关源码：

- `snail-ai-agent/snail-ai-agent-executor/snail-ai-agent-executor-core/src/main/java/com/aizuda/snail/ai/agent/core/interceptor/impl/LoggingInterceptor.java`

## TokenUsageCollectorAdvisor

`TokenUsageCollectorAdvisor` 从流式响应的最终 chunk 中提取 Token 使用量，并写入客户端流式执行上下文。

它依赖模型返回 usage 数据；OpenAI-compatible 流式调用通常需要启用 `streamUsage(true)` 才能在最终 chunk 中返回统计信息。

采集字段包括：

| 字段 | 说明 |
|------|------|
| `promptTokens` | 输入 Token 数 |
| `completionTokens` | 输出 Token 数 |
| `cacheTokens` | 缓存命中 Token 数，当前通过兼容性反射提取 |

相关源码：

- `snail-ai-agent/snail-ai-agent-executor/snail-ai-agent-executor-core/src/main/java/com/aizuda/snail/ai/agent/core/advisor/TokenUsageCollectorAdvisor.java`

## ThinkingCollectorAdvisor

`ThinkingCollectorAdvisor` 用于从流式响应 metadata 中累积模型的思维内容，并在流结束后写入 `AgentChatContextHolder.ChatContext` 和客户端流式执行上下文。

它会兼容以下 metadata key：

| Key | 说明 |
|-----|------|
| `reasoningContent` | 推理内容 |
| `thinking` | 思维内容 |
| `reasoning` | 推理内容 |
| `reasoning_content` | OpenAI-compatible 响应中的推理字段 |

相关源码：

- `snail-ai-agent/snail-ai-agent-executor/snail-ai-agent-executor-core/src/main/java/com/aizuda/snail/ai/agent/core/advisor/ThinkingCollectorAdvisor.java`

## 上下文传递

`AgentChatContextHolder` 保存当前对话执行上下文，`AgentChatContextThreadLocalAccessor` 负责在线程切换时传递上下文。

相关源码：

- `snail-ai-agent/snail-ai-agent-common/src/main/java/com/aizuda/snail/ai/agent/common/context/AgentChatContextHolder.java`
- `snail-ai-agent/snail-ai-agent-common/src/main/java/com/aizuda/snail/ai/agent/common/context/AgentChatContextThreadLocalAccessor.java`
