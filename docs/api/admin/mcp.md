# MCP API

MCP（Model Context Protocol）模块提供 MCP 服务的注册、管理、连接测试以及与智能体的关联配置能力。

---

## 获取 MCP 服务分页列表

```
GET /snail-ai/mcp-server/page
```

**请求参数：**

| 参数             | 类型     | 必填 | 说明                              |
|------------------|----------|------|-----------------------------------|
| `page`           | `number` | 否   | 页码，默认 `1`                    |
| `size`           | `number` | 否   | 每页条数，默认 `10`               |
| `keyword`        | `string` | 否   | 关键词搜索                        |
| `status`         | `number` | 否   | 状态过滤                          |
| `transportType`  | `number` | 否   | 传输类型：`1`=STDIO `2`=SSE `3`=StreamableHTTP |
| `datetimeRange`  | `string[]` | 否 | 时间范围过滤                      |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/mcp-server/page?page=1&size=10' \
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
        "name": "文件系统 MCP",
        "description": "提供文件读写能力的 MCP 服务",
        "transportType": 1,
        "baseUri": null,
        "endpoint": "",
        "command": "npx",
        "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data"],
        "envVars": null,
        "version": "1.0.0",
        "authType": 0,
        "authConfig": null,
        "status": "connected",
        "capabilities": ["tools", "resources"],
        "lastConnectDt": "2025-06-01 10:00:00",
        "createDt": "2025-03-01 10:00:00",
        "updateDt": "2025-06-01 10:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 3
  }
}
```

---

## 获取 MCP 服务详情

```
GET /snail-ai/mcp-server/{id}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/mcp-server/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

## 获取全部 MCP 服务列表

获取所有 MCP 服务（不分页），用于智能体配置时的下拉选择。

```
GET /snail-ai/mcp-server/list
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/mcp-server/list' \
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
      "name": "文件系统 MCP",
      "status": "connected",
      "capabilities": ["tools", "resources"]
    },
    {
      "id": 2,
      "name": "数据库查询 MCP",
      "status": "connected",
      "capabilities": ["tools"]
    }
  ]
}
```

---

## 创建 MCP 服务

```
POST /snail-ai/mcp-server
```

**请求体：**

| 字段            | 类型       | 必填 | 说明                                                |
|-----------------|------------|------|-----------------------------------------------------|
| `name`          | `string`   | 是   | 服务名称                                            |
| `description`   | `string`   | 否   | 描述                                                |
| `transportType` | `number`   | 否   | 传输类型：`1`=STDIO `2`=SSE `3`=StreamableHTTP     |
| `baseUri`       | `string`   | 否   | 基础 URI（SSE/HTTP 模式）                           |
| `endpoint`      | `string`   | 否   | 端点路径                                            |
| `command`        | `string`   | 否   | 启动命令（STDIO 模式）                              |
| `args`          | `string[]` | 否   | 命令参数                                            |
| `envVars`       | `object`   | 否   | 环境变量                                            |
| `authType`      | `number`   | 否   | 认证类型：`0`=无 `1`=Bearer `2`=API Key `3`=自定义 |
| `authConfig`    | `object`   | 否   | 认证配置                                            |
| `capabilities`  | `string[]` | 否   | 支持的能力列表                                      |

**curl 示例（STDIO 模式）：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/mcp-server' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "文件系统 MCP",
    "description": "提供文件读写能力",
    "transportType": 1,
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data"],
    "authType": 0
  }'
```

**curl 示例（SSE 模式）：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/mcp-server' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "远程工具 MCP",
    "description": "远程部署的 MCP 服务",
    "transportType": 2,
    "baseUri": "http://mcp-server.internal:3001",
    "endpoint": "/sse",
    "authType": 1,
    "authConfig": {"token": "your-bearer-token"}
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 4,
    "name": "文件系统 MCP",
    "transportType": 1,
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data"],
    "status": "disconnected",
    "createDt": "2025-06-01 10:00:00",
    "updateDt": "2025-06-01 10:00:00"
  }
}
```

---

## 更新 MCP 服务

```
PUT /snail-ai/mcp-server/{id}
```

请求体字段同创建接口。

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/mcp-server/1' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "文件系统 MCP v2",
    "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data", "/uploads"]
  }'
```

---

## 删除 MCP 服务

```
DELETE /snail-ai/mcp-server/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/mcp-server/1' \
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

## 测试 MCP 连接

测试 MCP 服务的连接状态，成功后会更新服务的状态和能力信息。

```
POST /snail-ai/mcp-server/{id}/test-connection
```

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/mcp-server/1/test-connection' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例（连接成功）：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "文件系统 MCP",
    "status": "connected",
    "capabilities": ["tools", "resources"],
    "lastConnectDt": "2025-06-01 10:30:00"
  }
}
```

**响应示例（连接失败）：**

```json
{
  "code": 0,
  "msg": "MCP 服务连接失败：Connection refused",
  "data": null
}
```

---

## 智能体 MCP 关联

### 获取智能体关联的 MCP 服务

```
GET /snail-ai/agent/{agentId}/mcp-servers
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/1/mcp-servers' \
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
      "name": "文件系统 MCP",
      "status": "connected",
      "capabilities": ["tools", "resources"]
    },
    {
      "id": 2,
      "name": "数据库查询 MCP",
      "status": "connected",
      "capabilities": ["tools"]
    }
  ]
}
```

---

### 更新智能体关联的 MCP 服务

```
PUT /snail-ai/agent/{agentId}/mcp-servers
```

**请求体：**

| 字段           | 类型       | 必填 | 说明                |
|----------------|------------|------|---------------------|
| `mcpServerIds` | `number[]` | 是   | MCP 服务 ID 列表    |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/agent/1/mcp-servers' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{"mcpServerIds": [1, 2, 3]}'
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

## 枚举值参考

### 传输类型（transportType）

| 值  | 说明              |
|-----|-------------------|
| `1` | STDIO（本地进程） |
| `2` | SSE（远程 SSE）   |
| `3` | StreamableHTTP    |

### 认证类型（authType）

| 值  | 说明       |
|-----|------------|
| `0` | 无认证     |
| `1` | Bearer     |
| `2` | API Key    |
| `3` | 自定义     |

### 连接状态（status）

| 值             | 说明       |
|----------------|------------|
| `connected`    | 已连接     |
| `disconnected` | 未连接     |
| `error`        | 连接异常   |
