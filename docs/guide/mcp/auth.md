# 认证配置

MCP 服务可能需要认证才能访问。Snail AI 支持四种认证方式，覆盖从无认证的内网服务到需要 OAuth 授权的第三方平台。

<!-- screenshot: mcp-auth-config.png — MCP 服务认证配置界面，展示不同认证类型的表单 -->

## 认证方式总览

| 认证方式 | 类型编号 | `authType` | 适用场景 | `authConfig` 结构 |
|----------|----------|------------|----------|-------------------|
| **无认证** | `0` | `0` | 内网服务、本地 Stdio 工具 | `null` |
| **API Key** | `1` | `1` | 需要 API Key 认证的服务 | `{ "key": "...", "value": "..." }` |
| **Bearer Token** | `2` | `2` | 标准 Token 认证 | `{ "token": "..." }` |
| **OAuth** | `3` | `3` | OAuth 2.0 客户端凭证 | `{ "clientId": "...", "clientSecret": "...", "tokenUrl": "..." }` |

## 无认证（type=0）

### 说明

不需要任何认证凭证，直接连接 MCP Server。

### 适用场景

- 部署在内网的 MCP 服务，通过网络隔离保证安全
- 本地 Stdio 类型的 MCP 工具
- 开发/测试环境的 MCP 服务

### 配置示例

```json
{
  "name": "内网数据服务",
  "transportType": 1,
  "endpoint": "http://10.0.1.100:8900/sse",
  "authType": 0,
  "authConfig": null
}
```

::: tip 说明
`authType` 为 `0` 时，`authConfig` 字段可以省略或设为 `null`。
:::

## API Key 认证（type=1）

### 说明

通过在 HTTP 请求头中附加自定义的 Key-Value 对实现认证。这是最常见的 API 认证方式之一。

### authConfig 结构

```typescript
{
  "key": string,    // 请求头名称
  "value": string   // API Key 值
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `key` | `string` | 是 | HTTP 请求头名称，如 `X-API-Key`、`Authorization` |
| `value` | `string` | 是 | API Key 的值 |

### 工作原理

Snail AI 在向 MCP Server 发送请求时，会自动在 HTTP 请求头中添加配置的 Key-Value 对：

```http
GET /sse HTTP/1.1
Host: mcp.example.com
X-API-Key: your-api-key-123456
```

### 配置示例

#### 示例一：自定义 Header

```json
{
  "name": "天气服务",
  "transportType": 1,
  "endpoint": "https://mcp-weather.example.com/sse",
  "authType": 1,
  "authConfig": {
    "key": "X-API-Key",
    "value": "sk-weather-xxxxxxxxxxxxxxxxxx"
  }
}
```

对应的请求头：

```http
X-API-Key: sk-weather-xxxxxxxxxxxxxxxxxx
```

#### 示例二：使用 Authorization Header

```json
{
  "authType": 1,
  "authConfig": {
    "key": "Authorization",
    "value": "ApiKey sk-xxxxxxxxxxxxxxxxxx"
  }
}
```

对应的请求头：

```http
Authorization: ApiKey sk-xxxxxxxxxxxxxxxxxx
```

### 适用场景

- MCP Server 使用自定义请求头验证 API Key
- 第三方服务提供了 API Key 但不使用标准 Bearer Token 格式
- 需要灵活指定请求头名称的认证方案

## Bearer Token 认证（type=2）

### 说明

使用标准的 Bearer Token 认证方式，在 HTTP 请求的 `Authorization` 头中携带 Token。这是 REST API 中最广泛使用的认证标准。

### authConfig 结构

```typescript
{
  "token": string   // Bearer Token 值
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | `string` | 是 | Token 值（不需要包含 "Bearer " 前缀） |

### 工作原理

Snail AI 自动在请求头中添加标准的 Bearer Token：

```http
GET /sse HTTP/1.1
Host: mcp.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 配置示例

```json
{
  "name": "企业知识搜索",
  "transportType": 2,
  "endpoint": "https://search-mcp.internal.com/mcp",
  "authType": 2,
  "authConfig": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtY3Atc2VydmljZSJ9.xxxxx"
  }
}
```

::: warning Token 填写注意
`token` 字段只需填写 Token 本身的值，**不要**包含 `Bearer ` 前缀。系统会自动拼接为 `Authorization: Bearer {token}` 格式。
:::

### 适用场景

- MCP Server 使用标准 JWT Token 认证
- 企业内部使用统一身份认证平台签发的 Token
- 与符合 RFC 6750 规范的服务对接

### 与 API Key 认证的区别

| 对比项 | API Key (type=1) | Bearer Token (type=2) |
|--------|------------------|----------------------|
| Header 名称 | 自定义（`key` 字段指定） | 固定为 `Authorization` |
| Header 值格式 | 自定义（`value` 字段原样传递） | 自动添加 `Bearer ` 前缀 |
| 灵活度 | 高，可用于任何自定义 Header | 遵循标准规范 |
| 规范性 | 非标准 | 符合 RFC 6750 |

## OAuth 认证（type=3）

### 说明

使用 OAuth 2.0 客户端凭证模式（Client Credentials Grant）进行认证。Snail AI 会自动向 Token 端点申请访问令牌，并在后续请求中携带该令牌。

### authConfig 结构

```typescript
{
  "clientId": string,       // OAuth 客户端 ID
  "clientSecret": string,   // OAuth 客户端密钥
  "tokenUrl": string        // Token 端点地址
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `clientId` | `string` | 是 | OAuth 应用的客户端 ID |
| `clientSecret` | `string` | 是 | OAuth 应用的客户端密钥 |
| `tokenUrl` | `string` | 是 | OAuth Token 端点 URL |

### 工作原理

```
Snail AI                    OAuth Server                MCP Server
   │                            │                           │
   │── POST /oauth/token ─────▶│                           │
   │   (clientId + secret)      │                           │
   │◀── access_token ──────────│                           │
   │                            │                           │
   │──────── GET /sse ──────────────────────────────────────▶│
   │         Authorization: Bearer {access_token}           │
   │◀─────── SSE 响应 ─────────────────────────────────────│
   │                                                        │
   │   (Token 过期后自动重新申请)                             │
```

**详细流程：**

1. Snail AI 使用 `clientId` 和 `clientSecret` 向 `tokenUrl` 发起 Token 请求
2. OAuth Server 验证凭证后返回 `access_token`
3. Snail AI 在后续 MCP 请求中自动携带该 Token
4. Token 过期时自动重新申请

### 配置示例

```json
{
  "name": "企业数据平台 MCP",
  "transportType": 2,
  "endpoint": "https://data-mcp.enterprise.com/mcp",
  "authType": 3,
  "authConfig": {
    "clientId": "mcp-snail-ai-client",
    "clientSecret": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenUrl": "https://auth.enterprise.com/oauth/token"
  }
}
```

### 适用场景

- MCP Server 受企业 IAM（身份与访问管理）系统保护
- 使用 Keycloak、Auth0、Azure AD 等身份平台管理访问
- 需要 Token 自动刷新机制的场景
- 严格的企业安全合规要求

### Token 请求格式

Snail AI 发送的 OAuth Token 请求遵循标准格式：

```http
POST /oauth/token HTTP/1.1
Host: auth.enterprise.com
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=mcp-snail-ai-client&client_secret=a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

## 认证方式选择指南

根据 MCP Server 的安全要求选择合适的认证方式：

| 场景 | 推荐方式 | 说明 |
|------|----------|------|
| 本地 Stdio 工具 | 无认证 (type=0) | 本地进程无需网络认证 |
| 内网服务（可信网络） | 无认证 (type=0) | 通过网络隔离保证安全 |
| 简单的 API Key 认证 | API Key (type=1) | 最简单的认证方式 |
| 标准 REST API 认证 | Bearer Token (type=2) | 符合行业标准规范 |
| 企业级安全要求 | OAuth (type=3) | Token 自动刷新，集中权限管理 |

## authConfig 完整对照表

| authType | authConfig 示例 | 说明 |
|----------|----------------|------|
| `0` | `null` | 无认证 |
| `1` | `{"key": "X-API-Key", "value": "sk-xxx"}` | 自定义 Header Key-Value |
| `2` | `{"token": "eyJhbGci..."}` | Bearer Token |
| `3` | `{"clientId": "app-id", "clientSecret": "secret", "tokenUrl": "https://auth.example.com/token"}` | OAuth 2.0 Client Credentials |

## 安全建议

::: danger 密钥安全
- `authConfig` 中的敏感信息（API Key、Token、ClientSecret）在数据库中加密存储
- 定期轮换认证凭证，特别是 API Key 和 OAuth Client Secret
- 使用最小权限原则：OAuth 客户端只授予 MCP Server 所需的最小权限范围
- 不要在 MCP Server 的 `description` 字段中记录认证信息
:::

::: tip 生产环境建议
- 优先使用 OAuth 认证（type=3），支持 Token 自动刷新和集中管理
- 如果 MCP Server 支持 IP 白名单，配合使用可进一步提升安全性
- 在企业环境中，建议通过统一的密钥管理服务（如 HashiCorp Vault）管理密钥
:::

## 常见问题

### Q: authConfig 填错了会怎样？

连接测试时会返回认证失败错误，MCP Server 的状态将显示为 `error`。可以通过编辑 MCP 服务修正 `authConfig` 后重新测试。

### Q: OAuth Token 过期了怎么办？

Snail AI 会自动检测 Token 过期并重新使用 `clientId` 和 `clientSecret` 申请新的 Token，无需手动干预。

### Q: Stdio 类型的 MCP 服务需要配置认证吗？

通常不需要。Stdio 类型的工具通过本地进程通信，不经过网络，因此一般设置 `authType: 0`（无认证）。如果 MCP 工具内部需要访问外部 API，应通过 `envVars` 传递所需的凭证。

### Q: 可以同时使用多种认证方式吗？

每个 MCP 服务只能配置一种认证方式。如果 MCP Server 需要多重认证（如 API Key + OAuth），请联系 MCP Server 的提供方确认首选认证方式。
