# 应用 API

应用（App）模块提供分布式客户端应用的管理能力，包括应用的注册、启停、客户端节点查询和踢出等功能。

---

## 获取应用分页列表

```
GET /snail-ai/app/page
```

**请求参数：**

| 参数            | 类型       | 必填 | 说明           |
|-----------------|------------|------|----------------|
| `page`          | `number`   | 否   | 页码，默认 `1` |
| `size`          | `number`   | 否   | 每页条数       |
| `keyword`       | `string`   | 否   | 关键词搜索     |
| `status`        | `number`   | 否   | 状态过滤       |
| `datetimeRange` | `string[]` | 否   | 时间范围过滤   |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/app/page?page=1&size=10' \
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
        "appId": "customer-service",
        "appName": "客服应用",
        "description": "企业客服对话应用",
        "token": "app-token-xxx",
        "routeStrategy": "ROUND_ROBIN",
        "status": 1,
        "onlineNodes": 3,
        "createDt": "2025-02-01 10:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 2
  }
}
```

---

## 获取已启用的应用列表

获取所有已启用的应用（不分页），用于智能体配置时的下拉选择。

```
GET /snail-ai/app/list
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/app/list' \
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
      "appId": "customer-service",
      "appName": "客服应用",
      "status": 1,
      "onlineNodes": 3
    }
  ]
}
```

---

## 创建应用

```
POST /snail-ai/app
```

**请求体：**

| 字段            | 类型     | 必填 | 说明                                          |
|-----------------|----------|------|-----------------------------------------------|
| `appId`         | `string` | 是   | 应用唯一标识                                  |
| `appName`       | `string` | 是   | 应用名称                                      |
| `description`   | `string` | 否   | 描述                                          |
| `routeStrategy` | `string` | 否   | 路由策略：`ROUND_ROBIN` / `RANDOM` / `HASH`  |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/app' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "appId": "data-analysis",
    "appName": "数据分析应用",
    "description": "专用于数据分析场景的客户端应用",
    "routeStrategy": "ROUND_ROBIN"
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 3,
    "appId": "data-analysis",
    "appName": "数据分析应用",
    "description": "专用于数据分析场景的客户端应用",
    "token": "auto-generated-token-xxx",
    "routeStrategy": "ROUND_ROBIN",
    "status": 1,
    "onlineNodes": 0,
    "createDt": "2025-06-01 10:00:00"
  }
}
```

---

## 更新应用

```
PUT /snail-ai/app/{id}
```

**请求体：**

| 字段            | 类型     | 必填 | 说明       |
|-----------------|----------|------|------------|
| `appName`       | `string` | 否   | 应用名称   |
| `description`   | `string` | 否   | 描述       |
| `routeStrategy` | `string` | 否   | 路由策略   |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/app/3' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "appName": "数据分析应用 v2",
    "routeStrategy": "HASH"
  }'
```

---

## 删除应用

```
DELETE /snail-ai/app/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/app/3' \
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

## 切换应用启停状态

```
POST /snail-ai/app/{id}/toggle-status
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/app/1/toggle-status' \
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

## 客户端节点

### 查询应用下的客户端节点

```
GET /snail-ai/app/{appId}/nodes
```

> 注意：此处路径参数为 `appId`（应用标识字符串），非数字 ID。

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/app/customer-service/nodes' \
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
      "appId": "customer-service",
      "appName": "客服应用",
      "hostId": "node-001",
      "hostIp": "192.168.1.10",
      "grpcPort": 50051,
      "maxConcurrent": 100,
      "activeChats": 35,
      "labels": {"region": "cn-east", "env": "production"},
      "expireDt": "2025-06-01 12:00:00",
      "online": true
    },
    {
      "id": 2,
      "appId": "customer-service",
      "appName": "客服应用",
      "hostId": "node-002",
      "hostIp": "192.168.1.11",
      "grpcPort": 50051,
      "maxConcurrent": 100,
      "activeChats": 42,
      "labels": {"region": "cn-east", "env": "production"},
      "expireDt": "2025-06-01 12:00:00",
      "online": true
    }
  ]
}
```

---

### 查询所有客户端节点（全局视图）

```
GET /snail-ai/app/all-nodes
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/app/all-nodes' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 踢出客户端节点

```
DELETE /snail-ai/app/{appId}/nodes/{hostId}
```

**路径参数：**

| 参数     | 类型     | 说明               |
|----------|----------|--------------------|
| `appId`  | `string` | 应用标识           |
| `hostId` | `string` | 客户端节点主机 ID  |

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/app/customer-service/nodes/node-002' \
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
