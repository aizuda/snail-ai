# 模型 API

模型管理模块提供 AI 模型提供商（Provider）和模型配置（Config）的完整管理能力，支持多提供商、多类型模型的统一接入，以及用量统计功能。

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
  "code": 1,
  "msg": "success",
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
  "code": 1,
  "msg": "success",
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
| `modelType`   | `string` | 否   | 模型类型：`CHAT`/`EMBEDDING`/`RERANKER`/`IMAGE`/`SPEECH` |
| `scope`       | `string` | 否   | 作用范围：`GLOBAL`/`PERSONAL`                      |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/configs?pageNum=1&pageSize=10&modelType=CHAT' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "providerId": 1,
        "modelName": "GPT-4o",
        "modelKey": "gpt-4o",
        "modelType": "CHAT",
        "description": "OpenAI 最新多模态模型",
        "apiKey": "sk-***",
        "apiEndpoint": "https://api.openai.com/v1",
        "configJson": {"temperature": 0.7, "maxTokens": 4096},
        "scope": "GLOBAL",
        "isDefault": true,
        "isEnabled": true,
        "createdDt": "2025-01-15 10:00:00",
        "updatedDt": "2025-06-01 12:00:00"
      }
    ],
    "total": 8,
    "size": 10,
    "current": 1,
    "pages": 1
  }
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
| `modelType`   | `string`  | 是   | 类型：`CHAT`/`EMBEDDING`/`RERANKER`/`IMAGE`/`SPEECH` |
| `apiKey`      | `string`  | 是   | API Key                                            |
| `apiEndpoint` | `string`  | 否   | API 端点 URL                                       |
| `description` | `string`  | 否   | 描述                                               |
| `configJson`  | `object`  | 否   | 额外配置参数                                       |
| `scope`       | `string`  | 否   | 作用范围：`GLOBAL`/`PERSONAL`                      |
| `isDefault`   | `boolean` | 否   | 是否设为默认                                       |
| `isEnabled`   | `boolean` | 否   | 是否启用                                           |

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
    "apiKey": "sk-your-api-key",
    "apiEndpoint": "https://api.openai.com/v1",
    "description": "轻量级多模态模型",
    "configJson": {"temperature": 0.7},
    "scope": "GLOBAL",
    "isEnabled": true
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
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
    "configJson": {"temperature": 0.5, "maxTokens": 2048}
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

## 按类型和提供商查询

### 按类型获取模型列表

```
GET /snail-ai/ai-model/by-type/{modelType}
```

**路径参数：** `modelType` - 模型类型（`CHAT`/`EMBEDDING`/`RERANKER`/`IMAGE`/`SPEECH`）

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/by-type/EMBEDDING' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
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
  "code": 1,
  "msg": "success",
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
  "code": 1,
  "msg": "success",
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
