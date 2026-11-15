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

它依赖模型返回 usage 数据；OpenAI-compatible 流式调用通常需要启用 `streamUsage(true)` 才能在最终 chunk 中返回统计信息，Responses 则使用其原生 usage 事件。

采集字段包括：

| 字段 | 说明 |
|------|------|
| `promptTokens` | 输入 Token 数 |
| `completionTokens` | 输出 Token 数 |
| `cacheTokens` | 缓存读取/命中 Token 数 |

缓存用量优先读取 Spring AI 通用 `Usage.getCacheReadInputTokens()`。如果旧 OpenAI-compatible 响应没有提供通用字段，再从 native usage 的 `promptTokensDetails.cachedTokens` 兼容性反射提取。负数按 0 处理，超出整数范围时截断为 `Integer.MAX_VALUE`。

相关源码：

- `snail-ai-agent/snail-ai-agent-executor/snail-ai-agent-executor-core/src/main/java/com/aizuda/snail/ai/agent/core/advisor/TokenUsageCollectorAdvisor.java`

## ThinkingCollectorAdvisor

`ThinkingCollectorAdvisor` 用于从流式响应中累积模型的思维内容，并在流结束后写入 `AgentChatContextHolder.ChatContext` 和客户端流式执行上下文。Responses 优先读取 Spring AI `ReasoningPart`：先取 `text()` 增量，没有 text 时再取 `summary()`。

当响应没有 `ReasoningPart` 时，才回退到原有 metadata 和附加属性提取逻辑：

它会兼容以下 metadata key：

| Key | 说明 |
|-----|------|
| `reasoningContent` | 推理内容 |
| `thinking` | 思维内容 |
| `reasoning` | 推理内容 |
| `reasoning_content` | OpenAI-compatible 响应中的推理字段 |

部分兼容服务会在每个流式 chunk 中返回从开头累计的 `reasoningContent`。Advisor 只追加相对已收集内容的新后缀，避免思考内容重复。原有思考回调、`fullThinking` 返回值、上下文桥接和对话记录持久化行为保持不变。

相关源码：

- `snail-ai-agent/snail-ai-agent-executor/snail-ai-agent-executor-core/src/main/java/com/aizuda/snail/ai/agent/core/advisor/ThinkingCollectorAdvisor.java`

## 上下文传递

`AgentChatContextHolder` 保存当前对话执行上下文，`AgentChatContextThreadLocalAccessor` 负责在线程切换时传递上下文。

相关源码：

- `snail-ai-agent/snail-ai-agent-common/src/main/java/com/aizuda/snail/ai/agent/common/context/AgentChatContextHolder.java`
- `snail-ai-agent/snail-ai-agent-common/src/main/java/com/aizuda/snail/ai/agent/common/context/AgentChatContextThreadLocalAccessor.java`
