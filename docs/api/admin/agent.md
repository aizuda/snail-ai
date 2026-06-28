# 智能体 API

智能体（Agent）是 Snail AI 的核心资源，提供创建、配置、对话、数据分析和市场订阅等能力。

## 获取智能体分页列表

```
GET /snail-ai/agent/page
```

**请求参数：**

| 参数       | 类型      | 必填 | 说明                            |
|------------|-----------|------|---------------------------------|
| `page`     | `number`  | 否   | 页码，默认 `1`                  |
| `size`     | `number`  | 否   | 每页条数，默认 `10`             |
| `keyword`  | `string`  | 否   | 关键词搜索（名称/描述）         |
| `featured` | `boolean` | 否   | 是否只查精选推荐                |
| `sort`     | `string`  | 否   | 排序方式：`latest` / `popular`  |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/page?page=1&size=10&keyword=助手' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "data": [
      {
        "id": 1,
        "name": "智能客服助手",
        "description": "企业客服场景的智能对话助手",
        "avatar": "https://example.com/avatar.png",
        "creator": "admin",
        "viewCount": 1280,
        "isFeatured": true,
        "instruction": "你是一个专业的客服助手...",
        "greeting": "你好，我是智能客服，有什么可以帮您？",
        "presetQuestions": ["如何退款？", "订单查询", "售后服务"],
        "chatModelId": 5,
        "chatModel": "gpt-4o",
        "mcpEnabled": true,
        "skillEnabled": false,
        "webSearchEnabled": false,
        "ragEnabled": true,
        "memoryEnabled": true,
        "shortTermMemorySize": 20,
        "ragId": 3,
        "memoryConfigId": 1,
        "appId": null,
        "status": 1,
        "subscribed": false,
        "createDt": "2025-01-15 10:30:00",
        "updateDt": "2025-03-20 14:22:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 25
  }
}
```

---

## 获取智能体详情

```
GET /snail-ai/agent/{id}
```

**路径参数：**

| 参数 | 类型     | 说明       |
|------|----------|------------|
| `id` | `number` | 智能体 ID  |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "智能客服助手",
    "description": "企业客服场景的智能对话助手",
    "avatar": "https://example.com/avatar.png",
    "creator": "admin",
    "viewCount": 1280,
    "isFeatured": true,
    "instruction": "你是一个专业的客服助手，负责回答用户关于产品和服务的问题。",
    "greeting": "你好，我是智能客服，有什么可以帮您？",
    "presetQuestions": ["如何退款？", "订单查询"],
    "chatModelId": 5,
    "mcpEnabled": true,
    "mcpServers": [],
    "skillEnabled": false,
    "skills": [],
    "ragEnabled": true,
    "ragId": 3,
    "memoryEnabled": true,
    "memoryConfigId": 1,
    "shortTermMemorySize": 20,
    "status": 1,
    "createDt": "2025-01-15 10:30:00",
    "updateDt": "2025-03-20 14:22:00"
  }
}
```

---

## 更新智能体

```
PUT /snail-ai/agent/{id}
```

**路径参数：**

| 参数 | 类型     | 说明       |
|------|----------|------------|
| `id` | `number` | 智能体 ID  |

**请求体：**

| 字段                 | 类型       | 必填 | 说明                                    |
|----------------------|------------|------|-----------------------------------------|
| `name`               | `string`   | 否   | 智能体名称                              |
| `description`        | `string`   | 否   | 描述                                    |
| `instruction`        | `string`   | 否   | 系统提示词                              |
| `greeting`           | `string`   | 否   | 欢迎语                                  |
| `presetQuestions`    | `string[]` | 否   | 预设问题列表                            |
| `avatar`             | `string`   | 否   | 头像 URL                                |
| `chatModelId`        | `number`   | 否   | 对话模型 ID                             |
| `mcpEnabled`         | `boolean`  | 否   | 是否启用 MCP                            |
| `mcpServerIds`       | `number[]` | 否   | 关联的 MCP 服务 ID 列表                 |
| `skillEnabled`       | `boolean`  | 否   | 是否启用技能                            |
| `skillIds`           | `number[]` | 否   | 关联的技能 ID 列表                      |
| `webSearchEnabled`   | `boolean`  | 否   | 是否启用联网搜索                        |
| `ragEnabled`         | `boolean`  | 否   | 是否启用 RAG 知识库                     |
| `ragId`              | `number`   | 否   | 绑定的知识库 ID                         |
| `memoryEnabled`      | `boolean`  | 否   | 是否启用记忆                            |
| `memoryConfigId`     | `number`   | 否   | 记忆配置 ID                             |
| `shortTermMemorySize`| `number`   | 否   | 短期记忆窗口大小，默认 `20`             |
| `isFeatured`         | `boolean`  | 否   | 是否精选推荐                            |
| `appId`              | `string`   | 否   | 执行应用 ID                             |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/agent/1' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "智能客服助手 v2",
    "instruction": "你是一个升级版的客服助手...",
    "mcpEnabled": true,
    "mcpServerIds": [1, 2],
    "ragEnabled": true,
    "ragId": 3
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "智能客服助手 v2",
    "description": "企业客服场景的智能对话助手",
    "instruction": "你是一个升级版的客服助手...",
    "mcpEnabled": true,
    "ragEnabled": true,
    "ragId": 3,
    "createDt": "2025-01-15 10:30:00",
    "updateDt": "2025-06-01 09:00:00"
  }
}
```

---

## 删除智能体

```
DELETE /snail-ai/agent/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/agent/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

---

## AI 流式创建智能体

通过自然语言描述，由 AI 自动生成智能体配置（名称、描述、提示词、欢迎语、头像等），以 **流式推送** 方式返回各字段生成进度。

```
POST /snail-ai/agent/create/stream
```

**请求体：**

| 字段          | 类型     | 必填 | 说明             |
|---------------|----------|------|------------------|
| `description` | `string` | 是   | 自然语言描述     |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/agent/create/stream' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{"description": "帮我创建一个企业财报分析专家"}' \
  --no-buffer
```

**响应格式（流式文本）：**

```
[START]
[FIELD_DONE]name:企业财报分析专家
[FIELD_DONE]description:专业的财务报表分析助手...
[FIELD_DONE]instruction:你是一位资深的财务分析专家...
[FIELD_DONE]greeting:你好！我是企业财报分析专家...
[FIELD_DONE]avatar:https://example.com/generated-avatar.png
[DONE]42
```

**流式信号说明：**

| 信号              | 格式                          | 说明                         |
|-------------------|-------------------------------|------------------------------|
| `[START]`         | 无参数                        | 创建流开始                   |
| `[FIELD_DONE]`    | `[FIELD_DONE]字段名:字段值`   | 某字段生成完成               |
| `[DONE]`          | `[DONE]agentId`               | 创建完成，返回智能体 ID      |
| `[ERROR]`         | `[ERROR]错误信息`             | 创建过程中出错               |

---

## 获取可用对话模型列表

```
GET /snail-ai/agent/chat-models
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/chat-models' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    { "id": 1, "value": "gpt-4o", "label": "GPT-4o" },
    { "id": 2, "value": "claude-sonnet-4-20250514", "label": "Claude Sonnet 4" },
    { "id": 3, "value": "deepseek-chat", "label": "DeepSeek Chat" }
  ]
}
```

---

## 智能体数据分析

```
GET /snail-ai/agent/{id}/analytics
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明                                       |
|---------|----------|------|--------------------------------------------|
| `range` | `string` | 否   | 时间范围：`1d` / `7d` / `30d` / `custom`  |
| `start` | `string` | 否   | 自定义起始时间（`range=custom` 时必填）    |
| `end`   | `string` | 否   | 自定义结束时间                              |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/1/analytics?range=7d' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "activeUsers": 156,
    "activeUsersTrend": [20, 22, 18, 25, 23, 24, 24],
    "conversationCount": 432,
    "conversationCountTrend": [55, 62, 58, 70, 65, 60, 62],
    "totalMessages": 2180,
    "messageTrend": [280, 310, 295, 350, 320, 315, 310],
    "totalToolCalls": 89,
    "toolCallTrend": [10, 15, 12, 14, 13, 12, 13],
    "avgResponseTime": 1.8,
    "dateLabels": ["06-01", "06-02", "06-03", "06-04", "06-05", "06-06", "06-07"],
    "dateRange": {
      "start": "2025-06-01",
      "end": "2025-06-07"
    }
  }
}
```

---

## 使用详情

```
GET /snail-ai/agent/{id}/usage-detail
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明           |
|---------|----------|------|----------------|
| `page`  | `number` | 否   | 页码           |
| `size`  | `number` | 否   | 每页条数       |
| `start` | `string` | 否   | 起始时间       |
| `end`   | `string` | 否   | 结束时间       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/1/usage-detail?page=1&size=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "data": [
      {
        "userId": 10,
        "userName": "张三",
        "department": "产品部",
        "messageCount": 45
      }
    ],
    "page": 1,
    "size": 10,
    "total": 8
  }
}
```

---

## 对话列表

获取智能体的对话摘要列表，用于分析和调试。

```
GET /snail-ai/agent/{id}/conversations
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明     |
|---------|----------|------|----------|
| `page`  | `number` | 否   | 页码     |
| `size`  | `number` | 否   | 每页条数 |
| `start` | `string` | 否   | 起始时间 |
| `end`   | `string` | 否   | 结束时间 |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/1/conversations?page=1&size=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "data": [
      {
        "conversationId": "conv-abc123",
        "title": "关于退款流程的咨询",
        "userName": "张三",
        "messageCount": 12,
        "toolCallCount": 3,
        "createDt": "2025-06-01 14:30:00",
        "lastMessageDt": "2025-06-01 14:45:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 432
  }
}
```

---

## 删除对话

```
DELETE /snail-ai/agent/{agentId}/conversation/{conversationId}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/agent/1/conversation/conv-abc123' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 获取对话消息

```
GET /snail-ai/agent/{agentId}/conversation/{conversationId}/messages
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/1/conversation/conv-abc123/messages' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "role": "user",
      "content": "如何申请退款？",
      "createDt": "2025-06-01 14:30:00"
    },
    {
      "role": "assistant",
      "content": "您可以通过以下步骤申请退款...",
      "thinking": "用户询问退款流程，需要查询知识库中的退款政策...",
      "status": 1,
      "createDt": "2025-06-01 14:30:05"
    }
  ]
}
```

---

## 智能体流式对话

与智能体进行流式对话，返回 NDJSON 格式的事件流。

```
POST /snail-ai/agent/{agentId}/chat
```

**请求体：**

| 字段             | 类型     | 必填 | 说明                         |
|------------------|----------|------|------------------------------|
| `conversationId` | `string` | 是   | 对话 ID，首次对话可生成 UUID |
| `content`        | `string` | 是   | 用户消息内容                 |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/agent/1/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "conversationId": "conv-abc123",
    "content": "帮我分析一下上季度的销售数据"
  }' \
  --no-buffer
```

**响应格式（NDJSON 流）：**

每行是一个 JSON 对象，通过 `type` 字段区分事件类型：

```json
{"type":"thinking","content":"用户需要分析销售数据，我应该先查询相关知识库..."}
{"type":"thinking","content":"找到了相关的销售报表数据..."}
{"type":"text","content":"根据"}
{"type":"text","content":"上季度的销售数据"}
{"type":"text","content":"分析如下：\n\n1. 总销售额同比增长 15%..."}
```

**事件类型：**

| type       | 说明                         |
|------------|------------------------------|
| `thinking` | 思考/推理过程（可选展示）    |
| `text`     | 正式回答内容，需拼接显示     |

---

## 智能体市场

获取公开市场中的智能体列表。

```
GET /snail-ai/agent/market
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/market' \
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
      "name": "企业财报解读专家",
      "description": "专业的财务报表分析助手",
      "avatar": "https://example.com/avatar1.png",
      "creator": "admin",
      "viewCount": 2500,
      "subscribed": false
    }
  ]
}
```

---

## 获取我的智能体

```
GET /snail-ai/agent/my
```

返回当前用户创建或订阅的智能体列表。

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/my' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 订阅智能体

```
POST /snail-ai/agent/{id}/subscribe
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/agent/1/subscribe' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 取消订阅智能体

```
DELETE /snail-ai/agent/{id}/subscribe
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/agent/1/subscribe' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```
