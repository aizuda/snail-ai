# 创建 MCP 服务

本章介绍如何在 Snail AI 中创建、配置和管理 MCP Server，以及如何将 MCP 服务绑定到智能体实现工具调用。

## 创建 MCP 服务

### 操作入口

在管理后台进入「MCP 集成」页面，点击「新建 MCP 服务」按钮。

### 基本信息

创建 MCP 服务时需要填写以下基本信息：

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `name` | 是 | 服务名称，如 "数据库查询工具"、"天气服务" |
| `description` | 否 | 服务描述，建议简要说明该服务提供的能力 |
| `transportType` | 是 | 传输协议类型：SSE (1) / Streamable HTTP (2) / Stdio (3) |

根据选择的传输协议类型，需要填写不同的连接参数。

### 创建 SSE 类型服务

<!-- screenshot: mcp-create-sse.png — 创建 SSE 类型 MCP 服务的表单页面 -->

SSE（Server-Sent Events）是一种基于 HTTP 长连接的通信方式，适用于远程部署的 MCP 服务。

**配置参数：**

| 字段 | 必填 | 说明 |
|------|------|------|
| `endpoint` | 是 | MCP Server 的 SSE 端点地址 |
| `authType` | 否 | 认证方式，默认无认证 |
| `authConfig` | 否 | 认证配置（根据 authType 填写） |

**创建示例：**

```json
POST /api/mcp-server

{
  "name": "天气查询服务",
  "description": "提供全球天气数据查询能力",
  "transportType": 1,
  "endpoint": "https://mcp-weather.example.com/sse",
  "authType": 1,
  "authConfig": {
    "key": "X-API-Key",
    "value": "your-api-key-here"
  }
}
```

### 创建 Streamable HTTP 类型服务

Streamable HTTP 与 SSE 类似，但使用标准 HTTP 请求进行流式通信，兼容性更好。

**配置参数：**

| 字段 | 必填 | 说明 |
|------|------|------|
| `endpoint` | 是 | MCP Server 的 HTTP 端点地址 |
| `authType` | 否 | 认证方式 |
| `authConfig` | 否 | 认证配置 |

**创建示例：**

```json
POST /api/mcp-server

{
  "name": "企业知识搜索",
  "description": "搜索内部知识库和文档",
  "transportType": 2,
  "endpoint": "https://mcp-search.internal.com/mcp",
  "authType": 2,
  "authConfig": {
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

### 创建 Stdio 类型服务

<!-- screenshot: mcp-create-stdio.png — 创建 Stdio 类型 MCP 服务的表单页面，包含命令、参数和环境变量配置 -->

Stdio（标准输入/输出）类型用于调用本地安装的 MCP 工具，通过启动一个本地进程并通过 stdin/stdout 进行通信。

**配置参数：**

| 字段 | 必填 | 说明 |
|------|------|------|
| `command` | 是 | 启动命令，如 `npx`、`uvx`、`node` |
| `args` | 否 | 命令参数数组 |
| `envVars` | 否 | 环境变量键值对 |

**创建示例：**

```json
POST /api/mcp-server

{
  "name": "文件系统工具",
  "description": "提供本地文件读写能力",
  "transportType": 3,
  "command": "npx",
  "args": [
    "-y",
    "@modelcontextprotocol/server-filesystem",
    "/data/workspace"
  ],
  "envVars": {
    "NODE_ENV": "production"
  }
}
```

更多 Stdio 配置示例：

```json
// Python MCP 工具
{
  "name": "数据分析工具",
  "transportType": 3,
  "command": "uvx",
  "args": ["mcp-server-pandas"],
  "envVars": {
    "DATA_DIR": "/data/datasets"
  }
}

// 自定义 Node.js MCP 工具
{
  "name": "自定义业务工具",
  "transportType": 3,
  "command": "node",
  "args": ["/opt/mcp-tools/my-business-tool/index.js"],
  "envVars": {
    "DB_HOST": "localhost",
    "DB_PORT": "3306"
  }
}
```

## 编辑 MCP 服务

点击服务列表中的「编辑」按钮，可以修改服务的所有配置项，包括传输类型、连接参数和认证信息。

```json
PUT /api/mcp-server/{id}

{
  "name": "天气查询服务（更新）",
  "description": "提供全球天气数据查询和预报能力",
  "transportType": 1,
  "endpoint": "https://mcp-weather-v2.example.com/sse",
  "authType": 1,
  "authConfig": {
    "key": "X-API-Key",
    "value": "new-api-key-here"
  }
}
```

## 删除 MCP 服务

```bash
DELETE /api/mcp-server/{id}
```

::: warning 删除须知
删除 MCP 服务前，请先从所有绑定的智能体中移除该服务。如果有智能体仍在使用该 MCP 服务，删除后相关工具调用将失败。
:::

## 连接测试

<!-- screenshot: mcp-test.png — MCP 连接测试结果页面，展示连接状态和发现的工具列表 -->

创建或修改 MCP 服务后，建议执行连接测试以验证配置是否正确：

```bash
POST /api/mcp-server/{id}/test-connection
```

连接测试会执行以下检查：

| 检查项 | 说明 |
|--------|------|
| **网络连通性** | 验证能否成功连接到 MCP Server |
| **协议握手** | 验证 MCP 协议握手是否成功 |
| **认证验证** | 如果配置了认证，验证凭证是否有效 |
| **工具发现** | 列出 MCP Server 提供的所有可用工具 |

测试成功后，服务状态将更新为 `connected`，同时返回该 MCP Server 提供的工具列表（`capabilities`）。

**测试成功响应示例：**

```json
{
  "id": 1,
  "name": "天气查询服务",
  "status": "connected",
  "capabilities": ["get_weather", "get_forecast", "get_air_quality"],
  "lastConnectDt": "2025-05-10T14:30:00"
}
```

**测试失败时**，服务状态将更新为 `error`，并在响应中返回错误信息，常见失败原因：

| 错误原因 | 排查方向 |
|----------|----------|
| 连接超时 | 检查 endpoint 地址是否正确、目标服务是否在线 |
| 认证失败 | 检查 authConfig 中的密钥/Token 是否有效 |
| 协议不兼容 | 检查 MCP Server 版本是否兼容、传输类型是否匹配 |
| 命令未找到 | Stdio 类型：检查 command 是否已安装并在 PATH 中 |

## 绑定 MCP 服务到智能体

MCP 服务创建完成后，需要绑定到智能体才能在对话中使用。

### 查看智能体绑定的 MCP 服务

```bash
GET /api/agent/{agentId}/mcp-servers
```

### 更新智能体的 MCP 绑定

```json
PUT /api/agent/{agentId}/mcp-servers

{
  "mcpServerIds": [1, 3, 5]
}
```

传入要绑定的 MCP Server ID 数组，系统会进行**全量替换**（不在数组中的绑定将被移除）。

### 通过智能体配置页面绑定

在智能体详情页的「MCP 服务」选项卡中：

1. 确保智能体开启了「MCP 工具」开关（`mcpEnabled: true`）
2. 在可用服务列表中勾选要绑定的 MCP 服务
3. 保存配置

::: tip 多服务绑定
一个智能体可以同时绑定多个 MCP 服务。对话时，所有绑定服务提供的工具都会注册到模型的工具列表中，模型会根据用户意图自动选择合适的工具。
:::

## 服务列表查询

### 分页查询

```bash
GET /api/mcp-server/page?page=1&size=10&keyword=天气&status=connected
```

支持的筛选参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| `page` | `number` | 页码，默认 1 |
| `size` | `number` | 每页条数，默认 10 |
| `keyword` | `string` | 搜索关键词（匹配名称和描述） |
| `status` | `string` | 连接状态过滤：`connected` / `disconnected` / `error` |
| `transportType` | `number` | 传输类型过滤：1 / 2 / 3 |

### 获取全部服务列表

```bash
GET /api/mcp-server/list
```

此接口返回所有 MCP 服务（不分页），主要用于智能体配置页面的 MCP 服务下拉选择框。

## 常见问题

### Q: SSE 和 Streamable HTTP 应该选哪个？

两者都适用于远程 MCP 服务。SSE 是 MCP 协议早期定义的传输方式，使用更广泛；Streamable HTTP 是新增的传输方式，兼容性更好。如果 MCP Server 同时支持两种方式，推荐使用 Streamable HTTP。详见 [传输协议](./transport)。

### Q: Stdio 类型的 MCP 服务需要在服务器上安装对应工具吗？

是的，Stdio 类型的 MCP 服务需要在 **Snail AI 后端所在服务器**上安装对应的命令行工具。例如使用 `npx` 启动的 MCP Server 需要服务器上安装 Node.js 环境。

### Q: 一个智能体最多可以绑定多少个 MCP 服务？

系统没有硬性数量限制，但绑定过多 MCP 服务会增加工具列表的长度，可能影响模型的工具选择准确性。建议根据智能体的实际需求精选绑定，一般不超过 5-10 个。
