# 记忆 API

记忆（Memory）模块提供对话记忆的检索、管理、提取、统计以及记忆配置（向量召回策略）的完整能力。支持按类型、时间、语义等多维度检索记忆，并可配置独立的记忆库实例。

---

## 记忆检索

### 获取对话记忆

获取指定对话关联的记忆列表。

```
GET /snail-ai/memory/conversation/{conversationId}
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明                  |
|---------|----------|------|-----------------------|
| `limit` | `number` | 否   | 返回数量上限，默认 `10` |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/conversation/conv-abc123?limit=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "agentId": 1,
      "userId": 10,
      "conversationId": "conv-abc123",
      "memoryType": "FACT",
      "category": "产品信息",
      "title": "用户偏好深色主题",
      "content": "用户在对话中表示偏好深色主题界面",
      "tags": ["UI", "偏好"],
      "relevanceScore": 0.95,
      "confidenceScore": 0.88,
      "status": "ACTIVE",
      "accessCount": 5,
      "accessedAt": "2025-06-01 14:30:00",
      "createDt": "2025-05-20 10:00:00",
      "updateDt": "2025-06-01 14:30:00"
    }
  ]
}
```

---

### 按类型获取记忆

获取指定智能体的记忆，可按类型过滤。

```
GET /snail-ai/memory/agent/{agentId}
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明                                                          |
|---------|----------|------|---------------------------------------------------------------|
| `type`  | `string` | 否   | 记忆类型：`FACT`/`DECISION`/`PREFERENCE`/`TASK_PROGRESS`/`REFERENCE` |
| `limit` | `number` | 否   | 返回数量上限，默认 `20`                                       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/agent/1?type=PREFERENCE&limit=20' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 获取最近记忆

```
GET /snail-ai/memory/agent/{agentId}/recent
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明                    |
|---------|----------|------|-------------------------|
| `days`  | `number` | 否   | 最近天数，默认 `7`      |
| `limit` | `number` | 否   | 返回数量上限，默认 `10` |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/agent/1/recent?days=7&limit=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 搜索记忆

通过语义搜索检索相关记忆。

```
POST /snail-ai/memory/search
```

**请求体：**

| 字段      | 类型       | 必填 | 说明                  |
|-----------|------------|------|-----------------------|
| `query`   | `string`   | 是   | 搜索查询              |
| `agentId` | `number`   | 否   | 限定智能体范围        |
| `types`   | `string[]` | 否   | 限定记忆类型          |
| `limit`   | `number`   | 否   | 返回数量上限          |
| `days`    | `number`   | 否   | 限定时间范围（天）    |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/search' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "query": "用户的主题偏好",
    "agentId": 1,
    "types": ["PREFERENCE", "FACT"],
    "limit": 5
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "memoryType": "PREFERENCE",
      "title": "用户偏好深色主题",
      "content": "用户在对话中表示偏好深色主题界面",
      "relevanceScore": 0.95,
      "confidenceScore": 0.88,
      "status": "ACTIVE"
    }
  ]
}
```

---

### 获取记忆详情

```
GET /snail-ai/memory/{memoryId}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 记忆管理

### 更新记忆

```
PUT /snail-ai/memory/{memoryId}
```

**请求体：**

| 字段             | 类型       | 必填 | 说明           |
|------------------|------------|------|----------------|
| `title`          | `string`   | 否   | 标题           |
| `content`        | `string`   | 否   | 内容           |
| `memoryType`     | `string`   | 否   | 记忆类型       |
| `category`       | `string`   | 否   | 分类           |
| `tags`           | `string[]` | 否   | 标签列表       |
| `relevanceScore` | `number`   | 否   | 相关性评分     |
| `status`         | `string`   | 否   | 状态           |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/memory/1' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "title": "用户偏好深色主题（已确认）",
    "tags": ["UI", "偏好", "已确认"],
    "relevanceScore": 0.98
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "title": "用户偏好深色主题（已确认）",
    "tags": ["UI", "偏好", "已确认"],
    "relevanceScore": 0.98,
    "status": "ACTIVE",
    "updateDt": "2025-06-01 15:00:00"
  }
}
```

---

### 删除记忆

```
DELETE /snail-ai/memory/{memoryId}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/memory/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 归档记忆

将记忆标记为已归档状态。

```
POST /snail-ai/memory/{memoryId}/archive
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/1/archive' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 压制记忆

将记忆标记为压制状态，后续检索中不再主动返回。

```
POST /snail-ai/memory/{memoryId}/suppress
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/1/suppress' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 激活记忆

将已归档或已压制的记忆重新激活。

```
POST /snail-ai/memory/{memoryId}/activate
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/1/activate' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 记忆提取与统计

### 强制提取记忆

手动触发从指定对话中提取记忆。

```
POST /snail-ai/memory/agent/{agentId}/conversations/{conversationId}/extract
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/agent/1/conversations/conv-abc123/extract' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "extractedCount": 3,
    "memories": [
      {
        "id": 10,
        "memoryType": "FACT",
        "title": "用户所在公司为科技企业",
        "content": "用户提到他在一家人工智能科技公司工作",
        "relevanceScore": 0.85,
        "status": "ACTIVE"
      }
    ]
  }
}
```

---

### 获取记忆统计

```
GET /snail-ai/memory/agent/{agentId}/stats
```

**请求参数：**

| 参数   | 类型     | 必填 | 说明                  |
|--------|----------|------|-----------------------|
| `days` | `number` | 否   | 统计天数，默认 `30`   |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/agent/1/stats?days=30' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "totalMemories": 156,
    "byType": {
      "FACT": 60,
      "DECISION": 25,
      "PREFERENCE": 35,
      "TASK_PROGRESS": 20,
      "REFERENCE": 16
    },
    "mostUsed": [1, 5, 12, 3, 8],
    "retrievalEffectiveness": 0.82,
    "dateRange": {
      "start": "2025-05-01",
      "end": "2025-06-01"
    }
  }
}
```

---

### 上下文预览

预览智能体在对话中将使用的上下文信息（包含记忆和消息）。

```
GET /snail-ai/memory/agent/{agentId}/conversations/{conversationId}/context-preview
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/agent/1/conversations/conv-abc123/context-preview' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "messages": [
      {"role": "system", "content": "你是一个智能客服助手..."},
      {"role": "user", "content": "帮我查询订单状态"},
      {"role": "assistant", "content": "好的，请提供您的订单号。"}
    ],
    "memories": [
      {
        "id": 1,
        "title": "用户偏好深色主题",
        "content": "用户在对话中表示偏好深色主题界面",
        "memoryType": "PREFERENCE"
      }
    ],
    "estimatedTokens": 1250,
    "compressionApplied": false
  }
}
```

---

## 记忆配置

记忆配置定义了记忆库的向量召回策略，可创建多个独立的记忆库配置并绑定到不同智能体。

### 获取记忆配置列表

```
GET /snail-ai/memory/config
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/config' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "默认记忆配置",
      "status": 1,
      "vectorStoreInstanceId": 1,
      "embeddingModelId": 2,
      "dimensionOfVectorModel": 1536,
      "searchEngineEnable": false,
      "maxRecall": 10,
      "rewriteEnabled": true,
      "rerankEnabled": true,
      "rerankModelId": 3,
      "enterRerankCount": 20,
      "thresholdEnabled": true,
      "similarityThreshold": 0.6,
      "fusionStrategy": "RRF",
      "rrfK": 60,
      "extractionInterval": 5,
      "maxMemoriesPerExtraction": 10,
      "extractionModelId": 5,
      "memoryExpirationDays": 90,
      "createDt": "2025-01-01 00:00:00",
      "updateDt": "2025-06-01 12:00:00"
    }
  ]
}
```

---

### 获取记忆配置详情

```
GET /snail-ai/memory/config/{id}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/memory/config/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 创建记忆配置

```
POST /snail-ai/memory/config
```

**请求体：**

| 字段                        | 类型      | 必填 | 说明                               |
|-----------------------------|-----------|------|------------------------------------|
| `name`                      | `string`  | 是   | 配置名称                           |
| `embeddingModelId`          | `number`  | 是   | Embedding 模型 ID                  |
| `vectorStoreInstanceId`     | `number`  | 否   | 向量存储实例 ID                    |
| `dimensionOfVectorModel`    | `number`  | 否   | 向量维度                           |
| `searchEngineEnable`        | `boolean` | 否   | 是否启用搜索引擎                   |
| `searchEngineInstanceId`    | `number`  | 否   | 搜索引擎实例 ID                    |
| `maxRecall`                 | `number`  | 否   | 最大召回数                         |
| `rewriteEnabled`            | `boolean` | 否   | 是否启用查询改写                   |
| `rerankEnabled`             | `boolean` | 否   | 是否启用 Rerank                    |
| `rerankModelId`             | `number`  | 否   | Rerank 模型 ID                     |
| `enterRerankCount`          | `number`  | 否   | 进入 Rerank 的候选数               |
| `thresholdEnabled`          | `boolean` | 否   | 是否启用相似度阈值                 |
| `similarityThreshold`       | `number`  | 否   | 相似度阈值                         |
| `fusionStrategy`            | `string`  | 否   | 融合策略：`RRF` / `WEIGHTED_SUM`  |
| `denseWeight`               | `number`  | 否   | 稠密向量权重                       |
| `rrfK`                      | `number`  | 否   | RRF K 参数                         |
| `extractionInterval`        | `number`  | 否   | 提取间隔（轮次）                   |
| `maxMemoriesPerExtraction`  | `number`  | 否   | 每次提取最大记忆数                 |
| `extractionModelId`         | `number`  | 否   | 提取模型 ID                        |
| `customExtractionPrompt`    | `string`  | 否   | 自定义记忆提取提示词               |
| `memoryExpirationDays`      | `number`  | 否   | 记忆过期天数                       |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/config' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "客服记忆配置",
    "embeddingModelId": 2,
    "vectorStoreInstanceId": 1,
    "maxRecall": 5,
    "rerankEnabled": true,
    "rerankModelId": 3,
    "thresholdEnabled": true,
    "similarityThreshold": 0.7,
    "extractionInterval": 3,
    "memoryExpirationDays": 60
  }'
```

---

### 更新记忆配置

支持部分字段更新。

```
PUT /snail-ai/memory/config/{id}
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/memory/config/1' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "maxRecall": 8,
    "similarityThreshold": 0.65
  }'
```

---

### 删除记忆配置

```
DELETE /snail-ai/memory/config/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/memory/config/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 调试记忆配置

使用指定配置执行记忆检索调试，验证召回效果。

```
POST /snail-ai/memory/config/{id}/debug
```

**请求体：**

| 字段             | 类型     | 必填 | 说明       |
|------------------|----------|------|------------|
| `agentId`        | `number` | 是   | 智能体 ID  |
| `userId`         | `number` | 是   | 用户 ID    |
| `query`          | `string` | 是   | 查询内容   |
| `conversationId` | `string` | 否   | 对话 ID    |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/memory/config/1/debug' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "agentId": 1,
    "userId": 10,
    "query": "用户的主题偏好是什么？"
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "memoryType": "PREFERENCE",
      "title": "用户偏好深色主题",
      "content": "用户在对话中表示偏好深色主题界面",
      "relevanceScore": 0.95,
      "status": "ACTIVE"
    }
  ]
}
```

---

## 枚举值参考

### 记忆类型（memoryType）

| 值              | 说明     |
|-----------------|----------|
| `FACT`          | 事实     |
| `DECISION`      | 决策     |
| `PREFERENCE`    | 偏好     |
| `TASK_PROGRESS` | 任务进度 |
| `REFERENCE`     | 参考资料 |

### 记忆状态（status）

| 值           | 说明   |
|--------------|--------|
| `ACTIVE`     | 激活   |
| `ARCHIVED`   | 已归档 |
| `SUPPRESSED` | 已压制 |
