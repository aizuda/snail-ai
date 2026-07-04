# MCP API

MCP（Model Context Protocol）模块提供 MCP 服务的注册、管理、连接测试以及与智能体的关联配置能力。

> Breaking Change: MCP 服务配置已移除 `version`、`authType`、`authConfig`、`capabilities` 字段，新增 API 字段 `timeoutMs` 与 `headers`。数据库列名为 `timeout`、`headers`，已有数据库需要执行本文末尾的结构调整 SQL。

---

## 获取 MCP 服务分页列表

```
GET /snail-ai/mcp-server/page
```

**请求参数：**

| 参数             | 类型       | 必填 | 说明                                             |
|------------------|------------|------|--------------------------------------------------|
| `page`           | `number`   | 否   | 页码，默认 `1`                                   |
| `size`           | `number`   | 否   | 每页条数，默认 `10`                              |
| `keyword`        | `string`   | 否   | 关键词搜索                                       |
| `transportType`  | `number`   | 否   | 传输类型：`1`=SSE（已废弃） `2`=Streamable HTTP `3`=Stdio |
| `datetimeRange`  | `string[]` | 否   | 时间范围过滤                                     |

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
        "transportType": 3,
        "baseUri": null,
        "endpoint": null,
        "command": "npx",
        "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data"],
        "envVars": null,
        "timeoutMs": 60000,
        "headers": null,
        "lastConnectDt": "2026-07-04 10:00:00",
        "createDt": "2026-07-04 09:00:00",
        "updateDt": "2026-07-04 09:30:00"
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

---

## 获取全部 MCP 服务列表

获取所有 MCP 服务（不分页），用于智能体配置时的下拉选择。

```
GET /snail-ai/mcp-server/list
```

---

## 创建 MCP 服务

```
POST /snail-ai/mcp-server
```

**请求体：**

| 字段            | 类型       | 必填 | 说明                                      |
|-----------------|------------|------|-------------------------------------------|
| `name`          | `string`   | 是   | 服务名称                                  |
| `description`   | `string`   | 否   | 描述                                      |
| `transportType` | `number`   | 否   | 传输类型：`1`=SSE（已废弃） `2`=Streamable HTTP `3`=Stdio |
| `baseUri`       | `string`   | 否   | 基础 URI（Streamable HTTP 模式）          |
| `endpoint`      | `string`   | 否   | 端点路径                                  |
| `command`       | `string`   | 否   | 启动命令（Stdio 模式）                    |
| `args`          | `string[]` | 否   | 命令参数                                  |
| `envVars`       | `object`   | 否   | Stdio 环境变量                            |
| `timeoutMs`     | `number`   | 否   | MCP 客户端超时时间，单位毫秒，默认 `60000` |
| `headers`       | `object`   | 否   | Streamable HTTP 请求头                    |

`headers` 的 key 不能为空、不能重复（大小写不敏感），key/value 不能包含换行符。

**curl 示例（Stdio 模式）：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/mcp-server' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "文件系统 MCP",
    "description": "提供文件读写能力",
    "transportType": 3,
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data"],
    "timeoutMs": 60000
  }'
```

**curl 示例（Streamable HTTP 模式）：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/mcp-server' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "远程工具 MCP",
    "description": "远程部署的 MCP 服务",
    "transportType": 2,
    "baseUri": "http://mcp-server.internal:3001",
    "endpoint": "/mcp",
    "timeoutMs": 120000,
    "headers": {
      "Authorization": "Bearer your-token",
      "X-Tenant": "snail-ai"
    }
  }'
```

---

## 更新 MCP 服务

```
PUT /snail-ai/mcp-server/{id}
```

请求体字段同创建接口。更新传输类型时，会清空旧传输类型专属字段；`headers` 仅在 Streamable HTTP 模式保存。

---

## 删除 MCP 服务

```
DELETE /snail-ai/mcp-server/{id}
```

---

## 测试 MCP 连接

测试 MCP 服务连通性，成功后会更新 `lastConnectDt`。

```
POST /snail-ai/mcp-server/{id}/test-connection
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

## 智能体 MCP 关联

### 获取智能体关联的 MCP 服务

```
GET /snail-ai/agent/{agentId}/mcp-servers
```

### 更新智能体关联的 MCP 服务

```
PUT /snail-ai/agent/{agentId}/mcp-servers
```

**请求体：**

| 字段           | 类型       | 必填 | 说明             |
|----------------|------------|------|------------------|
| `mcpServerIds` | `number[]` | 是   | MCP 服务 ID 列表 |

---

## 枚举值参考

### 传输类型（transportType）

| 值  | 说明                  |
|-----|-----------------------|
| `1` | SSE（已废弃）         |
| `2` | Streamable HTTP       |
| `3` | Stdio（本地进程）     |

---

## 现有数据库升级参考

仓库当前只维护初始化 SQL。已有环境升级前请先备份数据库，再按实际数据库执行等价结构调整。

### MySQL

```sql
ALTER TABLE sai_mcp_server
    ADD COLUMN timeout BIGINT DEFAULT 60000 COMMENT '超时时间(毫秒)' AFTER env_vars,
    ADD COLUMN headers TEXT COMMENT '请求头(JSON对象)' AFTER timeout,
    DROP COLUMN version,
    DROP COLUMN auth_type,
    DROP COLUMN auth_config,
    DROP COLUMN capabilities;
```

### PostgreSQL

```sql
ALTER TABLE sai_mcp_server ADD COLUMN IF NOT EXISTS timeout BIGINT DEFAULT 60000;
ALTER TABLE sai_mcp_server ADD COLUMN IF NOT EXISTS headers TEXT;
ALTER TABLE sai_mcp_server DROP COLUMN IF EXISTS version;
ALTER TABLE sai_mcp_server DROP COLUMN IF EXISTS auth_type;
ALTER TABLE sai_mcp_server DROP COLUMN IF EXISTS auth_config;
ALTER TABLE sai_mcp_server DROP COLUMN IF EXISTS capabilities;
COMMENT ON COLUMN sai_mcp_server.timeout IS '超时时间(毫秒)';
COMMENT ON COLUMN sai_mcp_server.headers IS '请求头(JSON对象)';
```

### 达梦

```sql
ALTER TABLE sai_mcp_server ADD timeout BIGINT DEFAULT 60000;
ALTER TABLE sai_mcp_server ADD headers TEXT;
ALTER TABLE sai_mcp_server DROP COLUMN version;
ALTER TABLE sai_mcp_server DROP COLUMN auth_type;
ALTER TABLE sai_mcp_server DROP COLUMN auth_config;
ALTER TABLE sai_mcp_server DROP COLUMN capabilities;
COMMENT ON COLUMN sai_mcp_server.timeout IS 'MCP timeout milliseconds';
COMMENT ON COLUMN sai_mcp_server.headers IS 'MCP HTTP headers JSON object';
```
