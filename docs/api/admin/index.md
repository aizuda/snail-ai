# Admin API 概述

Admin API 是 Snail AI 平台的管理后台接口，提供智能体、知识库、模型、MCP 服务、技能、记忆、应用、资源及用户等模块的完整管理能力。

## Base URL

所有 Admin API 请求的基础路径：

```
{protocol}://{host}:{port}/snail-ai
```

**示例：**

```
http://localhost:8900/snail-ai
```

## 认证方式

所有 Admin API 请求需在 HTTP Header 中携带 JWT Token 进行身份认证：

```
Snail-Ai-Auth: <your-jwt-token>
```

**获取 Token：** 通过 `POST /snail-ai/user/login` 接口登录获取。

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/agent/page' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

## 通用响应格式

所有接口统一返回以下 JSON 结构：

```json
{
  "code": 1,
  "msg": "success",
  "data": ...
}
```

| 字段   | 类型          | 说明                                  |
|--------|---------------|---------------------------------------|
| `code` | `number`      | 状态码，`1` 表示成功，其他值表示失败 |
| `msg`  | `string`      | 提示信息                              |
| `data` | `any`         | 响应数据，具体类型由接口决定         |

**错误响应示例：**

```json
{
  "code": 0,
  "msg": "认证失败，请重新登录",
  "data": null
}
```

## 分页格式

分页接口统一使用以下请求参数和响应结构：

**请求参数：**

| 参数   | 类型     | 默认值 | 说明       |
|--------|----------|--------|------------|
| `page` | `number` | `1`    | 当前页码   |
| `size` | `number` | `10`   | 每页条数   |

**响应结构：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "data": [...],
    "page": 1,
    "size": 10,
    "total": 100
  }
}
```

| 字段    | 类型      | 说明         |
|---------|-----------|--------------|
| `data`  | `array`   | 当前页数据   |
| `page`  | `number`  | 当前页码     |
| `size`  | `number`  | 每页条数     |
| `total` | `number`  | 总记录数     |

> 部分模块（如模型管理）使用 `pageNum/pageSize` 参数名，并返回 `records/current/size/total/pages` 结构，详见各模块文档。

## 错误码

| 错误码 | 说明             |
|--------|------------------|
| `1`    | 请求成功         |
| `0`    | 通用业务错误     |
| `401`  | 认证失败或过期   |
| `403`  | 权限不足         |
| `404`  | 资源不存在       |
| `500`  | 服务器内部错误   |

## API 模块一览

| 模块                          | 路径前缀               | 说明                         |
|-------------------------------|------------------------|------------------------------|
| [智能体 API](./agent.md)     | `/agent`               | 智能体 CRUD、对话、分析、市场 |
| [RAG API](./rag.md)          | `/rag` `/document` `/chunk` `/store-instance` | 知识库、文档、切片、存储实例 |
| [模型 API](./model.md)       | `/ai-model`            | 模型提供商、模型配置、用量统计 |
| [MCP API](./mcp.md)          | `/mcp-server`          | MCP 服务管理、连接测试       |
| [技能 API](./skill.md)       | `/skill`               | 技能包管理、文件在线编辑     |
| [记忆 API](./memory.md)      | `/memory`              | 对话记忆检索、管理、配置     |
| [应用 API](./app.md)         | `/app`                 | 应用管理、客户端节点         |
| [资源 API](./resource.md)    | `/resource`            | 文件上传、预览、下载         |
| [用户 API](./user.md)        | `/user`                | 用户登录、管理、授权         |
