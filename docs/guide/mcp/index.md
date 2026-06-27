# MCP 集成

MCP（Model Context Protocol，模型上下文协议）是一种开放标准，用于在 AI 模型与外部工具/数据源之间建立标准化的通信桥梁。Snail AI 完整实现了 MCP 协议，使智能体能够通过统一接口调用各种外部工具和服务。

<!-- screenshot: mcp-list.png — MCP 服务列表页面，展示已接入的 MCP 服务及其连接状态 -->

## 什么是 MCP？

MCP（Model Context Protocol）由 Anthropic 提出，是连接 AI 模型与外部世界的标准化协议。它的核心思想是：

> 让 AI 模型不仅能理解和生成文本，还能通过标准接口调用外部工具、查询数据源、执行操作。

在传统的 AI 应用中，工具调用（Tool Calling / Function Calling）通常需要为每个工具硬编码接口适配层。MCP 通过定义统一的协议规范，实现了 **"一次接入，处处可用"**：

```
┌──────────────┐     MCP 协议     ┌──────────────────┐
│              │ ◀──────────────▶ │  MCP Server A     │
│              │                  │  （数据库查询）     │
│              │     MCP 协议     ├──────────────────┤
│  Snail AI    │ ◀──────────────▶ │  MCP Server B     │
│  智能体      │                  │  （Web 搜索）      │
│              │     MCP 协议     ├──────────────────┤
│              │ ◀──────────────▶ │  MCP Server C     │
│              │                  │  （文件操作）       │
└──────────────┘                  └──────────────────┘
```

## 为什么需要 MCP？

| 能力 | 没有 MCP | 有 MCP |
|------|----------|--------|
| **工具接入** | 每个工具需要单独编写适配代码 | 遵循标准协议，即插即用 |
| **工具发现** | 硬编码工具列表 | 运行时动态发现可用工具 |
| **跨平台复用** | 工具绑定特定 AI 平台 | 同一 MCP Server 可被多个平台使用 |
| **安全隔离** | 工具直接在主进程中运行 | 工具运行在独立进程/服务中，权限可控 |
| **生态共享** | 每个团队重复造轮子 | 社区共享 MCP Server，丰富工具生态 |

## Snail AI 的 MCP 实现

Snail AI 完整支持 MCP 协议的三种传输方式，覆盖远程服务和本地工具两大场景：

| 传输方式 | 类型编号 | 适用场景 | 说明 |
|----------|----------|----------|------|
| **SSE** | `type=1` | 远程 MCP 服务 | 基于 Server-Sent Events 的长连接通信 |
| **Streamable HTTP** | `type=2` | 远程 MCP 服务 | 基于标准 HTTP 的流式通信，更通用 |
| **Stdio** | `type=3` | 本地 MCP 工具 | 通过标准输入输出与本地进程通信 |

同时，Snail AI 支持四种认证方式，满足不同安全要求：

| 认证方式 | 类型编号 | 说明 |
|----------|----------|------|
| **无认证** | `type=0` | 适用于内网或本地服务 |
| **API Key** | `type=1` | 通过请求头传递 API Key |
| **Bearer Token** | `type=2` | 标准 Bearer Token 认证 |
| **OAuth** | `type=3` | OAuth 2.0 客户端凭证模式 |

## MCP 工作流程

在 Snail AI 中，MCP 的典型工作流程如下：

```
用户提问 → 智能体分析意图 → 发现可用 MCP 工具
                                    ↓
                          选择合适工具并生成参数
                                    ↓
                        通过 MCP 协议调用工具
                                    ↓
                          接收工具执行结果
                                    ↓
                        整合结果生成最终回答 → 返回用户
```

**具体步骤：**

1. **工具注册**：管理员在系统中创建 MCP Server 配置
2. **工具绑定**：将 MCP Server 绑定到目标智能体
3. **工具发现**：智能体在对话时自动获取绑定的 MCP Server 提供的工具列表
4. **工具调用**：AI 模型根据用户意图自动选择并调用合适的工具
5. **结果整合**：工具返回的结果被注入到对话上下文中，模型据此生成回答

## MCP Server 数据结构

```typescript
type McpServer = {
  id: number;                              // 服务 ID
  name: string;                            // 服务名称
  description: string;                     // 服务描述
  transportType: 1 | 2 | 3;               // 传输类型：1=SSE, 2=Streamable HTTP, 3=Stdio
  baseUri: string | null;                  // 基础 URI
  endpoint: string;                        // 端点地址（SSE / Streamable HTTP）
  command: string;                         // 启动命令（Stdio）
  args: string[];                          // 命令参数（Stdio）
  envVars: Record<string, string> | null;  // 环境变量（Stdio）
  version: string;                         // 协议版本
  authType: 0 | 1 | 2 | 3;               // 认证类型
  authConfig: Record<string, any> | null;  // 认证配置
  status: 'connected' | 'disconnected' | 'error';  // 连接状态
  capabilities: string[];                  // 支持的能力列表
  lastConnectDt: string | null;           // 最后连接时间
  createDt: string;                        // 创建时间
  updateDt: string;                        // 更新时间
};
```

## 功能导航

| 功能 | 说明 | 文档链接 |
|------|------|----------|
| 创建 MCP 服务 | 配置和管理 MCP Server | [创建 MCP 服务](./create) |
| 传输协议 | SSE / Streamable HTTP / Stdio 详细配置 | [传输协议](./transport) |
| 认证配置 | 四种认证方式的详细说明 | [认证配置](./auth) |

## MCP API 概览

| 操作 | 方法 | 端点 | 说明 |
|------|------|------|------|
| 服务列表（分页） | `GET` | `/mcp-server/page` | 分页查询 MCP 服务 |
| 服务列表（全部） | `GET` | `/mcp-server/list` | 获取全部服务（用于下拉选择） |
| 服务详情 | `GET` | `/mcp-server/{id}` | 获取单个服务详情 |
| 创建服务 | `POST` | `/mcp-server` | 新建 MCP Server |
| 更新服务 | `PUT` | `/mcp-server/{id}` | 更新配置 |
| 删除服务 | `DELETE` | `/mcp-server/{id}` | 删除服务 |
| 测试连接 | `POST` | `/mcp-server/{id}/test-connection` | 测试 MCP 连接 |
| 智能体绑定 | `GET` | `/agent/{agentId}/mcp-servers` | 查看智能体绑定的 MCP 服务 |
| 更新绑定 | `PUT` | `/agent/{agentId}/mcp-servers` | 更新智能体的 MCP 绑定 |

::: tip 与智能体的关系
MCP Server 与智能体是**多对多**关系：一个智能体可以绑定多个 MCP Server，一个 MCP Server 也可以被多个智能体共用。通过智能体配置页面的「MCP 服务」选项卡管理绑定关系。
:::
