# 传输协议

MCP 协议定义了多种传输方式（Transport），用于在 Snail AI 与 MCP Server 之间建立通信通道。Snail AI 支持全部三种标准传输协议，适配远程服务和本地工具两大场景。

## 传输类型总览

| 传输方式 | 类型编号 | 通信模式 | 部署位置 | 适用场景 |
|----------|----------|----------|----------|----------|
| **SSE** | `type=1` | HTTP 长连接（Server-Sent Events） | 远程服务器 | 远程 MCP 服务，需要实时推送 |
| **Streamable HTTP** | `type=2` | HTTP 流式请求/响应 | 远程服务器 | 远程 MCP 服务，通用 HTTP 环境 |
| **Stdio** | `type=3` | 标准输入/输出管道 | 本地进程 | 本地安装的 MCP 工具 |

## SSE（Server-Sent Events）

### 概述

SSE 传输通过 HTTP 长连接实现服务端到客户端的事件推送，是 MCP 协议最早支持的远程传输方式。客户端通过 POST 发送请求，通过 SSE 连接接收响应和通知。

### 工作原理

```
Snail AI                          MCP Server
   │                                  │
   │──── GET /sse (建立SSE连接) ─────▶│
   │◀─── SSE: endpoint URL ──────────│
   │                                  │
   │──── POST /message (调用工具) ───▶│
   │◀─── SSE: 工具执行结果 ──────────│
   │                                  │
   │◀─── SSE: 通知/状态更新 ─────────│
   │                                  │
```

### 配置参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `transportType` | 是 | 固定为 `1` | `1` |
| `endpoint` | 是 | MCP Server 的 SSE 端点地址 | `https://mcp.example.com/sse` |

### 配置示例

```json
{
  "name": "远程数据查询服务",
  "description": "通过 SSE 连接的远程数据查询 MCP 服务",
  "transportType": 1,
  "endpoint": "https://mcp-data.example.com/sse"
}
```

### 适用场景

- MCP Server 部署在远程服务器或云端
- 需要实时接收服务端推送的状态更新
- MCP Server 使用 Node.js/Python SDK 的 SSE 模式运行
- 与社区开源 MCP Server 对接（很多默认提供 SSE 端点）

### 注意事项

::: warning SSE 连接管理
- SSE 是长连接协议，防火墙或反向代理需要允许长连接（避免超时断开）
- 如果使用 Nginx 反向代理，需要配置 `proxy_buffering off` 以支持 SSE
- 建议设置合理的连接超时和重连机制
:::

**Nginx SSE 代理配置参考：**

```nginx
location /mcp-sse/ {
    proxy_pass http://mcp-server:8900/;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 86400s;  # 24小时
}
```

## Streamable HTTP

### 概述

Streamable HTTP 是 MCP 协议新增的传输方式，使用标准 HTTP 请求/响应模型进行通信，支持响应体的流式传输。相比 SSE，它不依赖长连接，对网络环境的兼容性更好。

### 工作原理

```
Snail AI                          MCP Server
   │                                  │
   │──── POST /mcp (初始化) ────────▶│
   │◀─── HTTP 200 (能力列表) ────────│
   │                                  │
   │──── POST /mcp (调用工具) ──────▶│
   │◀─── HTTP 200 (流式响应) ────────│
   │     chunk1: {"jsonrpc":"2.0"...} │
   │     chunk2: {"jsonrpc":"2.0"...} │
   │                                  │
```

### 配置参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `transportType` | 是 | 固定为 `2` | `2` |
| `endpoint` | 是 | MCP Server 的 HTTP 端点地址 | `https://mcp.example.com/mcp` |

### 配置示例

```json
{
  "name": "企业搜索服务",
  "description": "通过 Streamable HTTP 连接的企业内部搜索",
  "transportType": 2,
  "endpoint": "https://search-mcp.internal.com/mcp"
}
```

### 适用场景

- 网络环境对长连接支持不佳（如某些企业代理/网关）
- MCP Server 部署在 Serverless 平台（如 AWS Lambda、Cloudflare Workers）
- 需要更好的负载均衡支持（标准 HTTP 请求更容易做 LB）
- MCP Server 使用最新 SDK 且支持 Streamable HTTP

### 与 SSE 的对比

| 对比项 | SSE (type=1) | Streamable HTTP (type=2) |
|--------|--------------|--------------------------|
| 连接模式 | 长连接 | 短连接（按请求） |
| 服务端推送 | 原生支持 | 通过流式响应实现 |
| 负载均衡 | 需要会话保持 | 无状态，易于负载均衡 |
| 代理兼容性 | 需要特殊配置 | 标准 HTTP，兼容性好 |
| Serverless | 不适合 | 完美适配 |
| 协议成熟度 | MCP 早期标准 | MCP 新增标准 |

## Stdio（标准输入/输出）

### 概述

Stdio 传输通过启动一个本地子进程，利用进程的标准输入（stdin）和标准输出（stdout）进行通信。这是运行本地 MCP 工具的首选方式。

### 工作原理

```
Snail AI 后端服务器
┌──────────────────────────────────────────┐
│                                          │
│  Snail AI ◀───stdin/stdout───▶ MCP 进程  │
│                                          │
│  1. 启动进程：npx @mcp/server-xxx       │
│  2. 通过 stdin 发送 JSON-RPC 请求        │
│  3. 通过 stdout 接收 JSON-RPC 响应       │
│  4. 对话结束后可选择保持或终止进程        │
│                                          │
└──────────────────────────────────────────┘
```

### 配置参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `transportType` | 是 | 固定为 `3` | `3` |
| `command` | 是 | 启动命令 | `npx`、`uvx`、`node`、`python` |
| `args` | 否 | 命令参数数组 | `["-y", "@modelcontextprotocol/server-filesystem", "/data"]` |
| `envVars` | 否 | 环境变量键值对 | `{"API_KEY": "xxx", "DATA_DIR": "/data"}` |

### 配置示例

#### 示例一：文件系统工具（Node.js）

```json
{
  "name": "文件系统工具",
  "description": "提供文件读写、目录浏览等能力",
  "transportType": 3,
  "command": "npx",
  "args": [
    "-y",
    "@modelcontextprotocol/server-filesystem",
    "/data/workspace"
  ]
}
```

#### 示例二：数据库查询工具（Python）

```json
{
  "name": "PostgreSQL 查询",
  "description": "执行 SQL 查询并返回结果",
  "transportType": 3,
  "command": "uvx",
  "args": ["mcp-server-postgres"],
  "envVars": {
    "POSTGRES_HOST": "localhost",
    "POSTGRES_PORT": "5432",
    "POSTGRES_DB": "mydb",
    "POSTGRES_USER": "reader",
    "POSTGRES_PASSWORD": "secret"
  }
}
```

#### 示例三：Git 操作工具

```json
{
  "name": "Git 工具",
  "description": "提供 Git 仓库操作能力",
  "transportType": 3,
  "command": "uvx",
  "args": ["mcp-server-git", "--repository", "/data/repos/my-project"]
}
```

#### 示例四：自定义 Node.js 工具

```json
{
  "name": "自定义业务工具",
  "description": "公司内部业务逻辑工具",
  "transportType": 3,
  "command": "node",
  "args": ["/opt/mcp-tools/business-tool/dist/index.js"],
  "envVars": {
    "NODE_ENV": "production",
    "SERVICE_URL": "https://api.internal.com",
    "AUTH_TOKEN": "xxx"
  }
}
```

### 适用场景

- 使用社区发布的 npm/pip MCP 工具包
- 运行自定义开发的本地 MCP 工具
- 需要访问本地文件系统、数据库等资源
- 不希望工具以独立服务形式部署

### 注意事项

::: danger Stdio 安全须知
Stdio 类型的 MCP 工具直接在 Snail AI **后端服务器**上执行，请注意：
- 确保 `command` 指向可信的程序，避免执行恶意代码
- 通过 `args` 限制工具的访问范围（如文件系统工具限定目录）
- 敏感信息（如数据库密码）应通过 `envVars` 传递，不要硬编码在 `args` 中
- 在生产环境中，建议使用专用的低权限用户运行 Snail AI 后端
:::

::: warning 环境依赖
- 使用 `npx` 命令需要服务器安装 Node.js（建议 18+）
- 使用 `uvx` 命令需要服务器安装 Python（建议 3.10+）和 `uv` 包管理器
- 确保 `command` 在系统 PATH 中可用
- Docker 部署时，需要在容器镜像中安装对应的运行时环境
:::

## 传输方式选择指南

根据实际场景选择合适的传输方式：

```
                  MCP Server 在哪里？
                    /          \
               远程服务器      本地安装
                /                 \
     网络环境如何？           ──▶ Stdio (type=3)
       /        \
    正常网络    受限网络
      |         (代理/网关/Serverless)
      |              |
  SSE (type=1)   Streamable HTTP (type=2)
```

| 决策因素 | 推荐传输方式 |
|----------|-------------|
| MCP Server 在远程，网络环境正常 | SSE 或 Streamable HTTP |
| MCP Server 在远程，经过企业代理/网关 | Streamable HTTP |
| MCP Server 部署在 Serverless 平台 | Streamable HTTP |
| MCP 工具需要访问本地资源 | Stdio |
| 使用 npm/pip 安装的 MCP 工具包 | Stdio |
| 需要最佳兼容性和通用性 | Streamable HTTP |
