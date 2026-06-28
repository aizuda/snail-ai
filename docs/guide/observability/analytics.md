# 统计分析

统计分析用于查看单个智能体的使用情况。当前开源版本提供统计概览和用户使用明细两个接口。

## 访问入口

在智能体详情页中，切换到数据分析标签页查看统计数据。

## 当前支持指标

| 字段 | 说明 |
|------|------|
| `activeUsers` | 指定时间范围内的去重活跃用户数 |
| `activeUsersTrend` | 按日期聚合的活跃用户趋势 |
| `conversationCount` | 指定时间范围内的对话总数 |
| `conversationCountTrend` | 按日期聚合的对话数趋势 |
| `totalMessages` | 指定时间范围内的消息总数 |
| `messageTrend` | 按日期聚合的消息数趋势 |
| `totalToolCalls` | 工具调用总数，当前服务实现返回默认值 |
| `avgResponseTime` | 平均响应时间，当前服务实现返回默认值 |
| `dateLabels` | 趋势数据对应的日期标签 |
| `dateRange` | 实际查询的日期范围 |

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/agent/{agentId}/analytics` | 获取智能体统计概览 |
| GET | `/agent/{agentId}/usage-detail` | 获取按用户聚合的使用明细 |
| GET | `/agent/{agentId}/conversations` | 获取智能体对话列表 |

### 统计概览示例

```text
GET /agent/1/analytics?range=7d
```

### 用户明细示例

```text
GET /agent/1/usage-detail?page=1&size=20&start=2026-01-01&end=2026-01-31
```

## 相关源码

- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/AgentController.java`
- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/service/agent/AgentAnalyticsService.java`
- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/vo/agent/AgentAnalyticsVO.java`
