# 使用统计

Snail AI 内置模型使用统计功能，自动记录每次模型调用的关键指标，帮助管理员和用户了解模型消耗情况、优化资源分配。

<!-- screenshot: model-usage-stats.png — 模型使用统计页面，展示调用次数、成功率、Token 消耗等指标 -->

## 统计指标

系统为每个模型自动采集以下维度的统计数据：

| 指标 | 字段 | 类型 | 说明 |
|------|------|------|------|
| **总调用次数** | `totalCalls` | `number` | 该模型被调用的总次数 |
| **成功次数** | `successCalls` | `number` | 调用成功的次数 |
| **失败次数** | `failedCalls` | `number` | 调用失败的次数 |
| **成功率** | `successRate` | `number` | 成功调用占比（百分比），`successCalls / totalCalls * 100` |
| **总 Token 消耗** | `totalTokensUsed` | `number` | 累计消耗的 Token 数量（输入 + 输出） |
| **总费用** | `totalCost` | `number` | 累计产生的费用（基于厂商定价计算） |
| **平均响应时间** | `avgResponseTime` | `number` | 平均每次调用的响应耗时（毫秒） |
| **最后使用时间** | `lastUsedDt` | `timestamp` | 该模型最近一次被调用的时间 |

## 统计数据结构

完整的使用统计数据结构如下：

```typescript
type UsageStatVO = {
  id: number;
  modelId: number;            // 模型配置 ID
  modelName: string;          // 模型名称
  modelType: string;          // 模型类型
  providerId: number;         // 提供商 ID
  providerName: string;       // 提供商名称
  userId: number;             // 用户 ID（个人统计时有值）
  totalCalls: number;         // 总调用次数
  successCalls: number;       // 成功次数
  failedCalls: number;        // 失败次数
  successRate: number;        // 成功率（%）
  totalTokensUsed: number;    // 总 Token 消耗
  totalCost: number;          // 总费用
  avgResponseTime: number;    // 平均响应时间（ms）
  lastUsedDt: number;         // 最后使用时间（时间戳）
  createdDt: number;          // 记录创建时间
  updatedDt: number;          // 记录更新时间
};
```

## 单模型统计

查看某个具体模型的使用统计数据：

```bash
GET /api/ai-model/usage/stat/{modelId}
```

**响应示例：**

```json
{
  "id": 1,
  "modelId": 101,
  "modelName": "GPT-4o",
  "modelType": "CHAT",
  "providerId": 1,
  "providerName": "OpenAI",
  "userId": 0,
  "totalCalls": 12580,
  "successCalls": 12341,
  "failedCalls": 239,
  "successRate": 98.1,
  "totalTokensUsed": 45678900,
  "totalCost": 523.45,
  "avgResponseTime": 2340,
  "lastUsedDt": 1715356800000
}
```

通过这些数据可以快速了解：

- 模型调用是否正常（关注 `successRate`）
- Token 消耗速度是否在预期范围内
- 响应时间是否满足业务要求

## 用户级统计

普通用户可以查看自己的模型使用情况，了解个人消耗：

```bash
GET /api/ai-model/usage/user-stats?pageNum=1&pageSize=10
```

**响应示例：**

```json
{
  "records": [
    {
      "modelId": 101,
      "modelName": "GPT-4o",
      "modelType": "CHAT",
      "providerName": "OpenAI",
      "userId": 42,
      "totalCalls": 356,
      "successCalls": 350,
      "failedCalls": 6,
      "successRate": 98.3,
      "totalTokensUsed": 1234567,
      "totalCost": 15.67,
      "avgResponseTime": 2100,
      "lastUsedDt": 1715356800000
    },
    {
      "modelId": 201,
      "modelName": "text-embedding-3-small",
      "modelType": "EMBEDDING",
      "providerName": "OpenAI",
      "userId": 42,
      "totalCalls": 128,
      "successCalls": 128,
      "failedCalls": 0,
      "successRate": 100.0,
      "totalTokensUsed": 567890,
      "totalCost": 0.56,
      "avgResponseTime": 450,
      "lastUsedDt": 1715270400000
    }
  ],
  "total": 2,
  "size": 10,
  "current": 1,
  "pages": 1
}
```

用户级统计适用于以下场景：

- 用户了解自己的 API 额度消耗
- 按模型维度分析个人使用习惯
- 排查个人遇到的调用失败问题

## 全局管理统计

管理员可以查看所有用户、所有模型的汇总统计数据，用于全局运营监控：

```bash
GET /api/ai-model/usage/global-stats?pageNum=1&pageSize=10
```

::: warning 权限要求
全局统计接口仅限管理员（Admin）角色访问。普通用户调用此接口将返回权限错误。
:::

**响应结构与用户级统计相同**，但 `userId` 字段为聚合值或 `0`，表示全局维度的汇总数据。

全局统计适用于以下场景：

| 场景 | 关注指标 | 说明 |
|------|----------|------|
| **成本管控** | `totalCost` | 监控各模型的费用消耗，及时发现异常增长 |
| **容量规划** | `totalCalls`、`totalTokensUsed` | 评估模型调用趋势，规划 API 额度预算 |
| **质量监控** | `successRate`、`avgResponseTime` | 发现调用失败率升高或响应变慢的模型 |
| **模型对比** | 综合对比 | 对比不同模型的性价比，指导模型选型决策 |

## 统计数据应用场景

### 场景一：成本优化

通过对比不同模型的 `totalCost` 和 `totalCalls`，计算单次调用成本：

```
单次调用成本 = totalCost / totalCalls
```

如果发现某个模型的单次成本显著偏高，可以考虑：
- 降低 `configJson` 中的 `maxTokens` 限制
- 切换到同厂商的更经济模型（如 GPT-4o-mini 替代 GPT-4o）
- 优化 Prompt 长度，减少输入 Token

### 场景二：可靠性排查

当 `successRate` 低于预期阈值时，排查方向：

| 成功率范围 | 可能原因 | 排查建议 |
|-----------|----------|----------|
| 95%-99% | 偶发网络超时或限流 | 检查 `configJson.timeoutMs` 是否合理 |
| 80%-95% | API Key 额度不足或配置错误 | 检查 API Key 余额和端点配置 |
| < 80% | 严重配置问题或服务不可用 | 检查提供商服务状态，验证 API Key 有效性 |

### 场景三：性能评估

通过 `avgResponseTime` 对比不同模型的响应性能：

- 对话模型（CHAT）：一般在 1000-5000ms，取决于模型大小和生成长度
- 向量模型（EMBEDDING）：一般在 100-500ms
- 重排序模型（RERANKER）：一般在 200-1000ms

如果响应时间显著偏高，可能是网络链路问题或模型服务端负载过高。

## 注意事项

::: tip 统计更新频率
使用统计数据为**近实时更新**，每次模型调用完成后自动累计。但在高并发场景下，统计数据可能存在少量延迟。
:::

::: warning 数据范围
- 使用统计为**累计值**，记录模型自创建以来的所有调用数据
- 删除模型配置后，对应的统计数据将一并清除
- 禁用模型不影响历史统计数据的查看
:::
