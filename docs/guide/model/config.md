# 模型配置

模型配置用于定义 Snail AI 调用模型服务所需的模型标识、端点、密钥和高级参数。本文以当前源码已落地能力为准，避免把兼容接入误写成内置 Provider。

## 概览

当前源码已落地的调用规格：

| 能力 | 支持方式 | 典型用途 |
|------|----------|----------|
| Chat | OpenAI-compatible Chat | 智能体对话、工具调用后的回答生成 |
| Embedding | OpenAI-compatible Embedding | RAG 文档向量化、语义检索 |
| Rerank | Qwen/HTTP Rerank | 检索结果重排 |

Claude、Gemini、Ollama、火山引擎等服务如果提供 OpenAI-compatible 接口，可以按兼容端点验证接入；不要把它们理解为当前源码内置的一等 Provider。

## 配置数据结构

每条模型配置通常包含以下字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `providerId` | `number` | 是 | 所属提供商 ID |
| `modelName` | `string` | 是 | 页面展示名称，如 `GPT-4o` |
| `modelKey` | `string` | 是 | API 调用时使用的模型标识，如 `gpt-4o` |
| `modelType` | `string` | 是 | 当前建议使用 `CHAT`、`EMBEDDING`、`RERANKER` |
| `apiKey` | `string` | 是 | API 密钥，存储时加密 |
| `apiEndpoint` | `string` | 否 | API 根地址或兼容端点地址 |
| `configJson` | `object` | 否 | 高级参数，JSON 格式 |
| `scope` | `string` | 否 | 作用域，如全局或个人配置 |
| `isDefault` | `boolean` | 否 | 是否为该类型默认模型 |
| `isEnabled` | `boolean` | 否 | 是否启用 |
| `description` | `string` | 否 | 模型说明 |

## OpenAI-compatible Chat

```json
{
  "providerId": 1,
  "modelName": "GPT-4o",
  "modelKey": "gpt-4o",
  "modelType": "CHAT",
  "apiKey": "sk-xxxxxxxxxxxxxxxxxxxxxxxx",
  "apiEndpoint": "https://api.openai.com",
  "description": "OpenAI-compatible Chat 模型",
  "configJson": {
    "temperature": 0.7,
    "topP": 1.0,
    "maxTokens": 4096
  }
}
```

兼容服务示例：

```json
{
  "providerId": 1,
  "modelName": "DeepSeek Chat",
  "modelKey": "deepseek-chat",
  "modelType": "CHAT",
  "apiKey": "sk-xxxxxxxxxxxx",
  "apiEndpoint": "https://api.deepseek.com",
  "description": "通过 OpenAI-compatible 接口接入"
}
```

填写兼容端点前，请确认目标服务是否兼容 OpenAI Chat Completions 或当前源码使用的请求格式。

## OpenAI-compatible Embedding

```json
{
  "providerId": 1,
  "modelName": "Text Embedding 3 Small",
  "modelKey": "text-embedding-3-small",
  "modelType": "EMBEDDING",
  "apiKey": "sk-xxxxxxxxxxxxxxxxxxxxxxxx",
  "apiEndpoint": "https://api.openai.com",
  "configJson": {
    "dimensions": 1536,
    "batchSize": 64
  }
}
```

Embedding 维度必须与向量库配置一致。例如 PgVector 配置了 `dimensions: 1536`，就应选择输出 1536 维的 Embedding 模型或调整向量表配置。

## Qwen/HTTP Rerank

```json
{
  "providerId": 2,
  "modelName": "Qwen Rerank",
  "modelKey": "gte-rerank",
  "modelType": "RERANKER",
  "apiKey": "your-api-key",
  "apiEndpoint": "https://dashscope.aliyuncs.com",
  "description": "用于 RAG 检索结果重排"
}
```

Rerank 模型通常用于 RAG 检索后，对候选分片重新排序，提升最终上下文质量。

## configJson 高级参数

### Chat 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `temperature` | `number` | 生成随机性，值越高越随机 |
| `topP` | `number` | 核采样概率 |
| `maxTokens` | `number` | 最大输出 Token 数 |
| `timeoutMs` | `number` | 请求超时时间，单位毫秒 |

示例：

```json
{
  "temperature": 0.3,
  "topP": 0.9,
  "maxTokens": 8192,
  "timeoutMs": 60000
}
```

### Embedding 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `dimensions` | `number` | 向量维度 |
| `batchSize` | `number` | 批量处理大小 |

### Rerank 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `topN` | `number` | 重排后保留的结果数量 |
| `timeoutMs` | `number` | 请求超时时间，单位毫秒 |

## 默认模型管理

系统可为常用模型类型配置默认模型。默认模型通常用于：

- 创建智能体时未显式指定 Chat 模型。
- 创建知识库时未显式指定 Embedding 模型。
- RAG 链路需要默认 Rerank 模型时。

同一模型类型建议只保留一个默认模型，避免运行时选择不明确。

## 启用与禁用

禁用模型后：

- 不应再出现在新的模型选择列表中。
- 已绑定该模型的智能体或知识库需要尽快切换配置。
- 历史调用统计仍可保留用于审计和排查。

删除模型前，建议先禁用并观察一段时间。

## API Key 加密

Snail AI 会加密保存模型 API Key。生产环境必须设置并备份：

```bash
export SNAIL_AI_CRYPTO_KEY=your_32_char_secret_key
export SNAIL_AI_CRYPTO_IV=your_32_char_initial_vector
```

注意：

- 不要在 `description` 或 `configJson` 中填写密钥。
- 不要在已有加密数据后随意更换密钥和 IV。
- 更换密钥后，历史 API Key 可能无法解密，需要重新录入。

## 常见问题

### apiEndpoint 应该填什么？

填写模型服务的根地址或兼容端点地址，不要把具体业务路径和模型名拼进去。实际路径由模型适配代码决定。

### modelKey 和 modelName 有什么区别？

- `modelKey` 是发送给模型服务的真实模型标识。
- `modelName` 是在 Snail AI 界面展示的名称。

### 为什么不直接列出 Claude、Gemini、Ollama、火山引擎配置？

当前文档只描述源码已落地的调用规格。上述服务如果提供 OpenAI-compatible 接口，可以按兼容端点方式测试；如果需要原生协议适配，应以新增源码适配和测试结果为准。

## 相关源码

- `snail-ai-models/`
- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/`
- `snail-ai-starter/src/main/resources/application.yml`
- `docs/guide/model/index.md`
- `docs/guide/model/provider.md`
