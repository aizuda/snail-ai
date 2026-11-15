# 模型提供商

模型提供商（Provider）用于描述一个 AI 服务端点或厂商，协议适配器（Adapter）决定请求格式。CHAT 模型支持 OpenAI-compatible Chat Completions 和 OpenAI Responses，Embedding 使用 OpenAI-compatible，Rerank 使用 Qwen/HTTP Rerank。

## 当前支持状态

| Provider/能力 | 状态 | 说明 |
|---------------|------|------|
| OpenAI-compatible Chat | 已支持 | 通过 OpenAI 兼容协议创建 Chat 模型 |
| OpenAI Responses Chat | 已支持 | 通过 `openai-responses` 适配器调用 Responses API |
| OpenAI-compatible Embedding | 已支持 | 通过 OpenAI 兼容协议创建 Embedding 模型 |
| Qwen/HTTP Rerank | 已支持 | 用于 RAG 检索结果重排序 |
| New API 中转站 | 已支持 | 使用 `new-api` Provider，支持 Chat Completions、Responses、Embedding 和 Rerank |
| 自定义 Provider 元数据 | 已支持 | 可在管理后台维护名称、key、图标、启用状态 |
| Claude / Gemini / Ollama / 火山引擎 | 不作为当前源码内置一等 Provider 描述 | 如服务兼容 OpenAI API，可按兼容端点方式验证接入 |

## OpenAI-compatible Provider

大量模型服务支持 OpenAI API 兼容格式。接入时重点配置：

| 字段 | 说明 | 示例 |
|------|------|------|
| 提供商名称 | 页面展示名 | `DeepSeek` |
| 提供商标识 | 唯一 key | `deepseek` |
| API Endpoint | 服务端点 | `https://api.deepseek.com` |
| Model Key | 模型标识 | `deepseek-chat` |
| 模型类型 | 能力类型 | `CHAT` |

## OpenAI Responses

Provider 元数据不绑定具体 CHAT 协议。同一 Provider 下可以分别创建 `openai-compatible` 和 `openai-responses` 模型配置：

| 字段 | 示例 | 说明 |
|------|------|------|
| 提供商标识 | `openai` | 仍使用现有 Provider 标识 |
| API Endpoint | `https://api.openai.com/v1` | Responses 服务根地址（包含 API 版本前缀） |
| Model Key | `gpt-5` | 支持 Responses 的模型标识 |
| 模型类型 | `CHAT` | Responses 当前作为 CHAT 模型接入 |
| 适配器 | `openai-responses` | 使用 Responses 请求/响应格式 |

`openai-compatible` 仍是 CHAT 默认适配器。已有配置不会自动迁移，也不会在 Responses 调用失败后回退到 Chat Completions。

## New API 中转站

New API 使用 OpenAI 兼容协议接入。新增或选择 Provider 时必须使用固定标识 `new-api`，模型配置示例如下：

| 字段 | 示例 | 说明 |
|------|------|------|
| 提供商名称 | `New API` | 页面展示名 |
| 提供商标识 | `new-api` | 用于启用 New API 请求标识，必须保持此值 |
| API Endpoint | `https://new-api.example.com/v1` | 自建或已获授权的 New API 实例地址 |
| Model Key | `gpt-5` | 实例中已配置的模型标识 |
| 适配器 | `openai-compatible` 或 `openai-responses` | 按模型实际支持的接口选择 |

调用 New API 时，Snail AI 会在最终请求 URL 上追加 `source=snail-ai`。例如：

```text
https://new-api.example.com/v1/responses?source=snail-ai
```

已有 query 参数会被保留；已有 `source` 会统一替换为 `snail-ai`，重复经过拦截器也只保留一个标识。该行为仅对 `providerKey=new-api` 生效，其他 OpenAI-compatible Provider 的请求 URL 不变。

新安装环境会通过 MySQL、PostgreSQL 或达梦初始化脚本创建 New API Provider。已有环境可在管理后台手动新增，且无需修改数据库结构。

## 管理提供商

### 查看提供商列表

```bash
curl -X GET 'http://localhost:8900/snail-ai/ai-model/providers' \
  -H 'Snail-Ai-Auth: <admin-token>'
```

### 新增提供商

在管理后台进入「模型管理」，点击「新增提供商」，填写：

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| 提供商名称 | 是 | 页面显示名称 |
| 提供商标识 | 是 | 唯一 key，建议小写英文、数字和连字符 |
| 描述 | 否 | 备注信息 |
| 图标 URL | 否 | 用于界面展示 |

`providerKey` 创建后不建议修改，并且必须保持全局唯一。

## 接入 DeepSeek 示例

1. 创建 Provider：
   - 名称：`DeepSeek`
   - 标识：`deepseek`
2. 创建模型配置：
   - 模型类型：`CHAT`
   - API Endpoint：`https://api.deepseek.com`
   - Model Key：`deepseek-chat`
   - API Key：填写服务商密钥
3. 在模型列表点击「验证」，确认端点、密钥和模型标识可用。
4. 设置为默认 Chat 模型。
5. 创建智能体并发起对话验证。

## 注意事项

- 不要仅因为文档中出现某个厂商名称，就默认它是源码内置 Provider。
- 只有 Chat、Embedding、Rerank 是当前源码已落地的调用规格；CHAT 当前包含两个协议适配器。
- 接入新的非兼容协议时，需要新增对应模型 Provider 实现，而不只是新增 Provider 元数据。
- New API 地址应指向自建或已确认上游授权与合规责任的实例，不要使用来源不明的公网中转地址。

## 相关源码

- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/AiModelController.java`
- `snail-ai-models/snail-ai-model-chat/snail-ai-model-chat-provider-openai-compatible`
- `snail-ai-models/snail-ai-model-chat/snail-ai-model-chat-provider-openai-responses`
- `snail-ai-models/snail-ai-model-embedding/snail-ai-model-embedding-provider-openai-compatible`
- `snail-ai-models/snail-ai-model-rerank/snail-ai-model-rerank-provider-qwen`
- `snail-ai-models/snail-ai-model-common/src/main/java/com/aizuda/snail/ai/model/common/http/NewApiProviderRequestInterceptor.java`
