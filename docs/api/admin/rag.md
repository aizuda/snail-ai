# RAG 知识库 API

RAG（检索增强生成）模块提供知识库管理、文档上传与解析、切片管理、语义搜索及问答等完整能力。包含四组子接口：知识库（`/rag`）、文档（`/document`）、切片（`/chunk`）、存储实例（`/store-instance`）。

---

## 知识库管理

### 获取知识库分页列表

```
GET /snail-ai/rag/page
```

**请求参数：**

| 参数   | 类型     | 必填 | 说明           |
|--------|----------|------|----------------|
| `page` | `number` | 否   | 页码，默认 `1` |
| `size` | `number` | 否   | 每页条数       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/rag/page?page=1&size=10' \
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
        "name": "产品知识库",
        "description": "包含所有产品相关文档",
        "icon": "BookOutlined",
        "embeddingModelId": 2,
        "embeddingModelName": "text-embedding-3-small",
        "dimensionOfVectorModel": 1536,
        "vectorStoreInstanceId": 1,
        "searchEngineEnable": true,
        "searchEngineInstanceId": 2,
        "delimiter": "\n\n",
        "documentCount": 45,
        "chunkCount": 1280,
        "status": "active",
        "dedupStrategy": 2,
        "dedupAction": 0,
        "uploadConfirm": true,
        "createdDt": "2025-02-01 10:00:00",
        "updateDt": "2025-06-01 12:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 5
  }
}
```

---

### 获取知识库详情

```
GET /snail-ai/rag/{id}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/rag/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 创建知识库

```
POST /snail-ai/rag
```

**请求体：**

| 字段                     | 类型      | 必填 | 说明                                                          |
|--------------------------|-----------|------|---------------------------------------------------------------|
| `name`                   | `string`  | 是   | 知识库名称                                                    |
| `description`            | `string`  | 否   | 描述                                                          |
| `icon`                   | `string`  | 否   | 图标                                                          |
| `embeddingModelId`       | `number`  | 是   | Embedding 模型 ID                                             |
| `vectorStoreInstanceId`  | `number`  | 否   | 向量存储实例 ID                                               |
| `dimensionOfVectorModel` | `number`  | 否   | 向量维度                                                      |
| `rerankModelId`          | `number`  | 否   | Rerank 模型 ID                                                |
| `searchEngineEnable`     | `boolean` | 否   | 是否启用全文搜索引擎                                          |
| `searchEngineInstanceId` | `number`  | 否   | 搜索引擎实例 ID                                               |
| `chunkMode`              | `string`  | 否   | 切分模式：`default` / `delimiter` / `regex` / `smart`         |
| `maxChunkTokens`         | `number`  | 否   | 单片最大 Token 数                                             |
| `chunkOverlap`           | `number`  | 否   | 切片重叠 Token 数                                             |
| `customDelimiter`        | `string`  | 否   | 自定义分隔符（`chunkMode=delimiter` 时使用）                  |
| `chunkRegex`             | `string`  | 否   | 正则表达式（`chunkMode=regex` 时使用）                        |
| `chunkModelId`           | `number`  | 否   | 语义切片模型 ID（`chunkMode=smart` 时使用）                   |
| `mergeShortSegments`     | `boolean` | 否   | 是否合并短段落                                                |
| `imageOcr`               | `boolean` | 否   | 是否启用图片 OCR                                              |
| `dedupStrategy`          | `number`  | 否   | 去重策略：`0`=无 `1`=按名称 `2`=按内容 `3`=按名称或内容      |
| `dedupAction`            | `number`  | 否   | 冲突动作：`0`=拒绝 `1`=跳过 `2`=覆盖                         |
| `uploadConfirm`          | `boolean` | 否   | 是否需要上传前二次确认                                        |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/rag' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "产品知识库",
    "description": "公司产品相关技术文档",
    "embeddingModelId": 2,
    "vectorStoreInstanceId": 1,
    "chunkMode": "default",
    "maxChunkTokens": 512,
    "chunkOverlap": 50,
    "dedupStrategy": 2,
    "dedupAction": 0
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 6,
    "name": "产品知识库",
    "description": "公司产品相关技术文档",
    "embeddingModelId": 2,
    "documentCount": 0,
    "chunkCount": 0,
    "status": "active",
    "createdDt": "2025-06-01 10:00:00",
    "updateDt": "2025-06-01 10:00:00"
  }
}
```

---

### 更新知识库

```
PUT /snail-ai/rag/{id}
```

请求体字段同创建接口，所有字段均为可选。

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/rag/1' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "产品知识库 v2",
    "searchEngineEnable": true,
    "searchEngineInstanceId": 2
  }'
```

---

### 保存知识库检索配置

```
PUT /snail-ai/rag/{id}/config
```

**请求体：**

```json
{
  "chunkParams": {
    "mode": "default",
    "maxChunkTokens": 512,
    "chunkOverlap": 50,
    "mergeShortSegments": true,
    "imageOcr": false
  },
  "searchParams": {
    "resultCount": 5,
    "rerankEnabled": true,
    "rerankModelId": 3,
    "enterRerankCount": 20,
    "thresholdEnabled": true,
    "threshold": 0.6,
    "fusionStrategy": "RRF",
    "rrfK": 60,
    "questionRewrite": true
  },
  "modelParams": {
    "modelId": 5,
    "nearbySliceCount": 2,
    "prompt": "基于以下参考内容回答问题：\n{context}\n\n问题：{query}"
  }
}
```

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/rag/1/config' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{"searchParams":{"resultCount":5,"rerankEnabled":true}}'
```

---

### 删除知识库

```
DELETE /snail-ai/rag/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/rag/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 文档管理

### 获取文档分页列表

```
GET /snail-ai/document/page
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明       |
|---------|----------|------|------------|
| `ragId` | `number` | 否   | 知识库 ID  |
| `page`  | `number` | 否   | 页码       |
| `size`  | `number` | 否   | 每页条数   |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/document/page?ragId=1&page=1&size=10' \
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
        "id": 10,
        "ragId": 1,
        "name": "产品手册.pdf",
        "fileType": "pdf",
        "sourceType": "local",
        "sourcePath": "/uploads/product-manual.pdf",
        "status": "success",
        "progress": 100,
        "chunkCount": 85,
        "fileSize": "2.4MB",
        "createdAt": "2025-03-15 10:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 45
  }
}
```

---

### 获取文档详情

```
GET /snail-ai/document/{id}
```

---

### 上传预览

批量上传文件并进行去重检测预览，返回 `previewToken` 和每个文件的预测决策。

```
POST /snail-ai/document/upload/preview
```

**Content-Type:** `multipart/form-data`

**表单字段：**

| 字段            | 类型     | 必填 | 说明                                          |
|-----------------|----------|------|-----------------------------------------------|
| `ragId`         | `string` | 是   | 知识库 ID                                     |
| `files`         | `File[]` | 是   | 文件列表（支持 pdf/docx/md/txt/csv/xlsx 等）  |
| `dedupStrategy` | `number` | 否   | 去重策略                                      |
| `dedupAction`   | `number` | 否   | 冲突动作                                      |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/document/upload/preview' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -F 'ragId=1' \
  -F 'files=@/path/to/document1.pdf' \
  -F 'files=@/path/to/document2.docx' \
  -F 'dedupStrategy=2' \
  -F 'dedupAction=0'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "previewToken": "preview-token-abc123",
    "ragId": 1,
    "items": [
      {
        "tempResourceId": 101,
        "fileName": "document1.pdf",
        "fileType": "pdf",
        "fileSize": 2457600,
        "contentHash": "sha256:abc...",
        "decision": "NEW",
        "matchType": "NONE",
        "conflictDocumentId": null,
        "conflictDocumentName": null
      },
      {
        "tempResourceId": 102,
        "fileName": "document2.docx",
        "fileType": "docx",
        "fileSize": 1024000,
        "decision": "REJECT",
        "matchType": "BY_CONTENT",
        "conflictDocumentId": 10,
        "conflictDocumentName": "产品手册.pdf",
        "rejectReason": "内容与已有文档重复"
      }
    ]
  }
}
```

---

### 上传提交

根据预览结果确认最终决策并入库。

```
POST /snail-ai/document/upload/commit
```

**请求体：**

```json
{
  "previewToken": "preview-token-abc123",
  "items": [
    { "tempResourceId": 101, "decision": "NEW" },
    { "tempResourceId": 102, "decision": "SKIP" }
  ]
}
```

**decision 可选值：** `NEW`（新增）、`SKIP`（跳过）、`OVERWRITE`（覆盖）

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/document/upload/commit' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "previewToken": "preview-token-abc123",
    "items": [
      {"tempResourceId": 101, "decision": "NEW"},
      {"tempResourceId": 102, "decision": "SKIP"}
    ]
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "conflictChanged": false,
    "items": [
      {
        "id": 46,
        "ragId": 1,
        "name": "document1.pdf",
        "fileType": "pdf",
        "decision": "NEW",
        "matchType": "NONE"
      },
      {
        "name": "document2.docx",
        "decision": "SKIP",
        "matchType": "BY_CONTENT"
      }
    ]
  }
}
```

---

### 取消预览

删除临时资源并使 token 失效。

```
DELETE /snail-ai/document/upload/preview/{token}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/document/upload/preview/preview-token-abc123' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### URL 导入文档

```
POST /snail-ai/document/import/url
```

**请求体：**

| 字段    | 类型     | 必填 | 说明        |
|---------|----------|------|-------------|
| `ragId` | `number` | 是   | 知识库 ID   |
| `url`   | `string` | 是   | 文档 URL    |
| `name`  | `string` | 否   | 自定义名称  |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/document/import/url' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "ragId": 1,
    "url": "https://example.com/docs/api-guide.pdf",
    "name": "API 使用指南"
  }'
```

---

### 重新解析文档

触发文档重新进入解析流水线。

```
POST /snail-ai/document/{id}/reprocess
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/document/10/reprocess' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 预览文档

获取文档原始内容的二进制流，用于在线预览。

```
GET /snail-ai/document/{id}/preview
```

> 返回 `Content-Type` 为文件原始 MIME 类型的二进制流。

---

### 下载文档

```
GET /snail-ai/document/{id}/download
```

> 返回二进制流，带 `Content-Disposition` 头。

---

### 删除文档

```
DELETE /snail-ai/document/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/document/10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 切片管理

### 获取切片分页列表

```
GET /snail-ai/chunk/page
```

**请求参数：**

| 参数         | 类型     | 必填 | 说明       |
|--------------|----------|------|------------|
| `ragId`      | `number` | 否   | 知识库 ID  |
| `documentId` | `number` | 否   | 文档 ID    |
| `page`       | `number` | 否   | 页码       |
| `size`       | `number` | 否   | 每页条数   |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/chunk/page?ragId=1&documentId=10&page=1&size=20' \
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
        "id": 100,
        "ragId": 1,
        "documentId": 10,
        "documentName": "产品手册.pdf",
        "paragraphIndex": 0,
        "chunkIndex": 0,
        "content": "Snail AI 是一个开源的企业级 AI Agent 平台...",
        "tokenCount": 256,
        "vectorId": "vec-001",
        "vectorStatus": "success",
        "type": "text",
        "createdDt": "2025-03-15 10:05:00",
        "updateDt": "2025-03-15 10:05:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 85
  }
}
```

---

### 创建切片

```
POST /snail-ai/chunk
```

**请求体：**

| 字段         | 类型     | 必填 | 说明       |
|--------------|----------|------|------------|
| `ragId`      | `number` | 是   | 知识库 ID  |
| `documentId` | `number` | 是   | 文档 ID    |
| `content`    | `string` | 是   | 切片内容   |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/chunk' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "ragId": 1,
    "documentId": 10,
    "content": "这是手动添加的切片内容..."
  }'
```

---

### 更新切片

```
PUT /snail-ai/chunk/{id}
```

**请求体：**

| 字段      | 类型     | 必填 | 说明         |
|-----------|----------|------|--------------|
| `content` | `string` | 是   | 新的切片内容 |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/chunk/100' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{"content": "更新后的切片内容..."}'
```

---

### 删除切片

```
DELETE /snail-ai/chunk/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/chunk/100' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 知识库搜索

```
POST /snail-ai/rag/search
```

**请求体：**

| 字段    | 类型      | 必填 | 说明                |
|---------|-----------|------|---------------------|
| `ragId` | `number`  | 是   | 知识库 ID           |
| `query` | `string`  | 是   | 搜索查询            |
| `debug` | `boolean` | 否   | 是否返回调试信息    |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/rag/search' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "ragId": 1,
    "query": "退款政策是什么？",
    "debug": true
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "results": [
      {
        "chunkId": 105,
        "content": "用户可在购买后 7 日内申请无理由退款...",
        "score": 0.92,
        "documentId": 10,
        "documentName": "退款政策.md",
        "metadata": {}
      },
      {
        "chunkId": 108,
        "content": "退款审核周期为 3-5 个工作日...",
        "score": 0.85,
        "documentId": 10,
        "documentName": "退款政策.md",
        "metadata": {}
      }
    ],
    "metrics": {
      "embeddingMs": 120,
      "vectorSearchMs": 45,
      "bm25SearchMs": 30,
      "fusionMs": 5,
      "rerankMs": 200,
      "totalMs": 400,
      "vectorHitCount": 10,
      "bm25HitCount": 8,
      "finalCount": 5
    }
  }
}
```

---

## 知识库问答（流式）

基于知识库进行 RAG 问答，返回纯文本流。

```
POST /snail-ai/rag/qa/stream
```

**请求体：**

| 字段             | 类型     | 必填 | 说明                   |
|------------------|----------|------|------------------------|
| `ragId`          | `number` | 是   | 知识库 ID              |
| `query`          | `string` | 是   | 问题                   |
| `conversationId` | `string` | 否   | 对话 ID（多轮对话时传入） |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/rag/qa/stream' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "ragId": 1,
    "query": "退款流程是怎样的？"
  }' \
  --no-buffer
```

**响应格式：** 纯文本流，逐块返回生成内容。

```
根据知识库中的退款政策文档，
退款流程如下：

1. 在订单详情页点击"申请退款"
2. 填写退款原因和说明
3. 提交后等待审核（3-5 个工作日）
4. 审核通过后，退款将原路返回
```

---

## 存储实例管理

存储实例用于管理向量库和搜索引擎的连接配置。

### 获取存储实例列表

```
GET /snail-ai/store-instance
```

**请求参数：**

| 参数       | 类型     | 必填 | 说明                                |
|------------|----------|------|-------------------------------------|
| `category` | `number` | 否   | 类别：`1`=向量库 `2`=搜索引擎      |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/store-instance?category=1' \
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
      "name": "默认 PgVector",
      "category": "VECTOR_STORE",
      "type": "PG_VECTOR",
      "config": {
        "host": "localhost",
        "port": 5432,
        "database": "snail_ai"
      },
      "status": "ACTIVE",
      "isDefault": true,
      "createDt": "2025-01-01 00:00:00",
      "updateDt": "2025-01-01 00:00:00"
    }
  ]
}
```

---

### 存储实例分页查询

```
GET /snail-ai/store-instance/page
```

**请求参数：**

| 参数       | 类型     | 必填 | 说明       |
|------------|----------|------|------------|
| `page`     | `number` | 否   | 页码       |
| `size`     | `number` | 否   | 每页条数   |
| `category` | `number` | 否   | 类别       |
| `keyword`  | `string` | 否   | 关键词     |
| `type`     | `string` | 否   | 存储类型   |
| `status`   | `string` | 否   | 状态       |

---

### 获取存储实例详情

```
GET /snail-ai/store-instance/{id}
```

---

### 创建存储实例

```
POST /snail-ai/store-instance
```

**请求体：**

| 字段        | 类型     | 必填 | 说明                                                        |
|-------------|----------|------|-------------------------------------------------------------|
| `name`      | `string` | 是   | 实例名称                                                    |
| `category`  | `number` | 是   | 类别：`1`=VECTOR_STORE `2`=SEARCH_ENGINE                    |
| `type`      | `number` | 是   | 类型：`1`=PG_VECTOR `2`=MILVUS `3`=ELASTICSEARCH `4`=PG_FULLTEXT |
| `config`    | `object` | 是   | 连接配置                                                    |
| `status`    | `number` | 否   | 状态：`0`=INACTIVE `1`=ACTIVE                               |
| `isDefault` | `boolean`| 否   | 是否默认实例                                                |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/store-instance' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "Milvus 集群",
    "category": 1,
    "type": 2,
    "config": {
      "host": "milvus.internal",
      "port": 19530,
      "database": "default"
    },
    "status": 1,
    "isDefault": false
  }'
```

---

### 更新存储实例

```
PUT /snail-ai/store-instance/{id}
```

---

### 删除存储实例

```
DELETE /snail-ai/store-instance/{id}
```

---

### 测试存储实例连接

```
POST /snail-ai/store-instance/test
```

**请求体：**

| 字段     | 类型     | 必填 | 说明       |
|----------|----------|------|------------|
| `type`   | `string` | 是   | 存储类型   |
| `config` | `object` | 是   | 连接配置   |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/store-instance/test' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "type": "PG_VECTOR",
    "config": {
      "host": "localhost",
      "port": 5432,
      "database": "snail_ai",
      "username": "postgres",
      "password": "password"
    }
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": true
}
```
