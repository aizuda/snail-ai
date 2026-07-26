# 模型 API

模型管理模块提供 AI 模型提供商（Provider）和模型配置（Config）的完整管理能力，支持多提供商、多类型模型的统一接入、模型配置连通性验证，以及用量统计功能。

---

## 模型提供商

### 获取启用的提供商列表

```
GET /snail-ai/ai-model/providers
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/providers' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": [
    {
      "id": 1,
      "providerName": "OpenAI",
      "providerKey": "openai",
      "description": "OpenAI GPT 系列模型",
      "iconUrl": "https://example.com/icons/openai.svg",
      "isEnabled": true,
      "createdDt": "2025-01-01 00:00:00",
      "updatedDt": "2025-01-01 00:00:00"
    },
    {
      "id": 2,
      "providerName": "Anthropic",
      "providerKey": "anthropic",
      "description": "Claude 系列模型",
      "iconUrl": "https://example.com/icons/anthropic.svg",
      "isEnabled": true,
      "createdDt": "2025-01-01 00:00:00",
      "updatedDt": "2025-01-01 00:00:00"
    }
  ]
}
```

---

### 获取所有提供商（含禁用）

```
GET /snail-ai/ai-model/all-providers
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/all-providers' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 获取提供商详情

```
GET /snail-ai/ai-model/provider/{id}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/provider/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 创建提供商

```
POST /snail-ai/ai-model/provider
```

**请求体：**

| 字段           | 类型      | 必填 | 说明           |
|----------------|-----------|------|----------------|
| `providerName` | `string`  | 是   | 提供商名称     |
| `providerKey`  | `string`  | 是   | 唯一标识符     |
| `description`  | `string`  | 否   | 描述           |
| `iconUrl`      | `string`  | 否   | 图标 URL       |
| `isEnabled`    | `boolean` | 否   | 是否启用       |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/ai-model/provider' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "providerName": "DeepSeek",
    "providerKey": "deepseek",
    "description": "DeepSeek 大语言模型",
    "isEnabled": true
  }'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": 3
}
```

> 返回新创建的提供商 ID。

---

### 更新提供商

```
PUT /snail-ai/ai-model/provider/{id}
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/provider/3' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "providerName": "DeepSeek AI",
    "description": "DeepSeek 系列大语言模型"
  }'
```

---

### 删除提供商

```
DELETE /snail-ai/ai-model/provider/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/ai-model/provider/3' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 启用提供商

```
PUT /snail-ai/ai-model/provider/{id}/enable
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/provider/3/enable' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 禁用提供商

```
PUT /snail-ai/ai-model/provider/{id}/disable
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/provider/3/disable' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 模型适配器

### 获取模型类型支持的适配器列表

```
GET /snail-ai/ai-model/adapters
```

**请求参数：**

| 参数        | 类型     | 必填 | 说明 |
|-------------|----------|------|------|
| `modelType` | `string` | 是   | 模型类型：`CHAT`/`EMBEDDING`/`RERANKER` |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/adapters?modelType=CHAT' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": [
    {
      "adapterKey": "openai-compatible",
      "name": "OpenAI Compatible",
      "modelType": "CHAT",
      "isDefault": true
    }
  ]
}
```

> 当前默认适配器：`CHAT`/`EMBEDDING` 使用 `openai-compatible`，`RERANKER` 使用 `qwen-rerank`。

---

## 模型配置

### 获取模型配置列表（分页）

```
GET /snail-ai/ai-model/configs
```

**请求参数：**

| 参数          | 类型     | 必填 | 说明                                               |
|---------------|----------|------|----------------------------------------------------|
| `pageNum`     | `number` | 否   | 页码，默认 `1`                                     |
| `pageSize`    | `number` | 否   | 每页条数，默认 `10`                                |
| `providerKey` | `string` | 否   | 提供商标识过滤                                     |
| `modelType`   | `string` | 否   | 模型类型：当前调用支持 `CHAT`/`EMBEDDING`/`RERANKER` |
| `scope`       | `string` | 否   | 作用范围：`GLOBAL`/`PERSONAL`                      |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/configs?pageNum=1&pageSize=10&modelType=CHAT' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": [
    {
      "id": 1,
      "providerId": 1,
      "providerName": "OpenAI",
      "modelName": "GPT-4o",
      "modelKey": "gpt-4o",
      "modelType": "CHAT",
      "adapterKey": "openai-compatible",
      "description": "OpenAI 最新多模态模型",
      "apiKey": "sk-***",
      "apiEndpoint": "https://api.openai.com/v1",
      "configJson": {"temperature": 0.7, "maxTokens": 4096, "capabilities": ["vision"]},
      "scope": "GLOBAL",
      "isDefault": true,
      "isEnabled": true,
      "createdDt": "2026-01-15T10:00:00",
      "updatedDt": "2026-06-01T12:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 8
}
```

---

### 获取模型配置详情

```
GET /snail-ai/ai-model/config/{id}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/config/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 创建模型配置

```
POST /snail-ai/ai-model/config
```

**请求体：**

| 字段          | 类型      | 必填 | 说明                                               |
|---------------|-----------|------|----------------------------------------------------|
| `providerId`  | `number`  | 是   | 提供商 ID                                          |
| `modelName`   | `string`  | 是   | 模型显示名称                                       |
| `modelKey`    | `string`  | 是   | 模型标识（如 `gpt-4o`）                            |
| `modelType`   | `string`  | 是   | 类型：当前调用支持 `CHAT`/`EMBEDDING`/`RERANKER` |
| `adapterKey`  | `string`  | 否   | 底层协议适配器；为空时按模型类型使用默认适配器 |
| `apiKey`      | `string`  | 是   | API Key                                            |
| `apiEndpoint` | `string`  | 是   | API 端点 URL                                       |
| `description` | `string`  | 否   | 描述                                               |
| `configJson`  | `object`  | 否   | 额外配置参数；`capabilities` 包含 `vision` 时支持图片输入 |
| `scope`       | `string`  | 否   | 作用范围：`GLOBAL`/`PERSONAL`                      |
| `isDefault`   | `boolean` | 否   | 是否设为默认                                       |

**configJson 图片输入字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `capabilities` | `string[]` | 模型能力标记；包含 `vision` 时表示该 CHAT 模型支持图片输入识别 |
| `defaultVisionModel` | `boolean` | 是否作为 RAG Docling 图片 OCR 的默认视觉兜底模型 |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/ai-model/config' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "providerId": 1,
    "modelName": "GPT-4o Mini",
    "modelKey": "gpt-4o-mini",
    "modelType": "CHAT",
    "adapterKey": "openai-compatible",
    "apiKey": "sk-your-api-key",
    "apiEndpoint": "https://api.openai.com/v1",
    "description": "轻量级多模态模型",
    "configJson": {"temperature": 0.7, "capabilities": ["vision"]},
    "scope": "GLOBAL"
  }'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": 9
}
```

> 返回新创建的模型配置 ID。

---

### 更新模型配置

```
PUT /snail-ai/ai-model/config/{id}
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/config/9' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "modelName": "GPT-4o Mini (Updated)",
    "configJson": {"temperature": 0.5, "maxTokens": 2048, "capabilities": ["vision"]}
  }'
```

---

### 删除模型配置

```
DELETE /snail-ai/ai-model/config/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/ai-model/config/9' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 启用模型配置

```
PUT /snail-ai/ai-model/config/{id}/enable
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/config/9/enable' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 禁用模型配置

```
PUT /snail-ai/ai-model/config/{id}/disable
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/config/9/disable' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 验证模型配置连通性

```
POST /snail-ai/ai-model/config/{id}/test
```

**说明：**

此接口无请求体。服务端会读取指定模型配置，解密 API Key，并按模型类型发起一次最小探活请求：

| 模型类型 | 探活行为 | 支持适配器 |
|----------|----------|------------|
| `CHAT` | 发送单轮 `Hi` 消息 | `openai-compatible` |
| `EMBEDDING` | 对 `test` 文本生成一次向量 | `openai-compatible` |
| `RERANKER` | 使用单 query、单文档执行 top1 重排 | `qwen-rerank` |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/ai-model/config/9/test' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**成功响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": {
    "success": true,
    "message": "连接成功",
    "responseTimeMs": 5003
  }
}
```

**连通性失败响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": {
    "success": false,
    "message": "401 Unauthorized",
    "responseTimeMs": 612
  }
}
```

> API Key、端点、模型名或网络不可用导致的失败会作为诊断结果返回在 `data.success=false` 中；模型配置不存在等业务错误仍按全局异常响应返回。

---

## 按类型和提供商查询

### 按类型获取模型列表

```
GET /snail-ai/ai-model/by-type/{modelType}
```

**路径参数：** `modelType` - 模型类型（当前调用支持 `CHAT`/`EMBEDDING`/`RERANKER`）

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/by-type/EMBEDDING' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": [
    {
      "id": 2,
      "providerId": 1,
      "modelName": "text-embedding-3-small",
      "modelKey": "text-embedding-3-small",
      "modelType": "EMBEDDING",
      "isEnabled": true
    }
  ]
}
```

---

### 按提供商和类型查询

```
GET /snail-ai/ai-model/by-provider-type
```

**请求参数：**

| 参数          | 类型     | 必填 | 说明       |
|---------------|----------|------|------------|
| `providerKey` | `string` | 是   | 提供商标识 |
| `modelType`   | `string` | 是   | 模型类型   |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/by-provider-type?providerKey=openai&modelType=CHAT' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 默认模型

### 获取全局默认模型

```
GET /snail-ai/ai-model/default
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/default' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 按类型获取默认模型

```
GET /snail-ai/ai-model/default/{modelType}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/default/CHAT' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 切换默认模型

```
PUT /snail-ai/ai-model/switch-default/{modelId}
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/ai-model/switch-default/5' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": true
}
```

---

## 用量统计

### 获取模型使用统计

```
GET /snail-ai/ai-model/usage/stat/{modelId}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/usage/stat/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "status": 1,
  "message": "Request succeeded",
  "data": {
    "id": 1,
    "modelId": 1,
    "modelName": "GPT-4o",
    "modelType": "CHAT",
    "providerId": 1,
    "providerName": "OpenAI",
    "totalCalls": 15680,
    "successCalls": 15520,
    "failedCalls": 160,
    "successRate": 0.9898,
    "totalTokensUsed": 12500000,
    "totalCost": 125.50,
    "avgResponseTime": 1.8
  }
}
```

---

### 用户模型统计（分页）

```
GET /snail-ai/ai-model/usage/user-stats
```

**请求参数：**

| 参数       | 类型     | 必填 | 说明           |
|------------|----------|------|----------------|
| `pageNum`  | `number` | 否   | 页码，默认 `1` |
| `pageSize` | `number` | 否   | 每页条数       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/usage/user-stats?pageNum=1&pageSize=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 全局模型统计（仅管理员）

```
GET /snail-ai/ai-model/usage/global-stats
```

**请求参数：**

| 参数       | 类型     | 必填 | 说明           |
|------------|----------|------|----------------|
| `pageNum`  | `number` | 否   | 页码，默认 `1` |
| `pageSize` | `number` | 否   | 每页条数       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/usage/global-stats?pageNum=1&pageSize=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

> 此接口仅管理员（Admin）权限可访问。
