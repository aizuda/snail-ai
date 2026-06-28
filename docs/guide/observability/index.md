# 统计分析

当前开源版本提供智能体维度的统计分析能力，核心接口位于 `AgentController`：

```text
GET /snail-ai/agent/{id}/analytics
GET /snail-ai/agent/{id}/usage-detail
```

统计数据来自 `AgentAnalyticsService` 和 `AgentUsageStatMapper`，用于查看指定智能体在一段时间内的活跃用户、对话数、消息数和用户使用明细。

## 当前支持状态

| 能力 | 状态 | 相关源码 |
|------|------|----------|
| 智能体统计概览 | 已支持 | `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/AgentController.java` |
| 用户使用明细 | 已支持 | `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/service/agent/AgentAnalyticsService.java` |
| 客户端日志拦截 | 已支持 | `snail-ai-agent/snail-ai-agent-executor/snail-ai-agent-executor-core/src/main/java/com/aizuda/snail/ai/agent/core/interceptor/impl/LoggingInterceptor.java` |
| Token 与思维内容采集 | 已支持 | `TokenUsageCollectorAdvisor`、`ThinkingCollectorAdvisor` |

## 相关页面

- [统计分析](./analytics.md)
- [客户端日志](/guide/client/logging)
