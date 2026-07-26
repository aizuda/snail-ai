# 模型管理

Snail AI 提供统一的模型提供商与模型配置管理。当前源码已落地的模型调用规格主要包括 Chat、Embedding 和 Rerank；Provider 可以配置 OpenAI-compatible 端点来接入兼容 OpenAI API 的服务。CHAT 模型可通过模型配置标记 `vision` 能力，用于智能体对话图片输入和图片识别。

模型管理页上半部分展示模型提供商卡片，下半部分维护具体模型配置、启用状态、连通性验证和默认模型。

![模型管理页面](/images/model/model-management-overview.png)

## 当前支持状态

| 能力 | 状态 | 对应源码 |
|------|------|----------|
| Chat 模型 | 已支持，OpenAI-compatible；可配置 `vision` 图片输入能力 | `snail-ai-models/snail-ai-model-chat/snail-ai-model-chat-provider-openai-compatible` |
| Embedding 模型 | 已支持，OpenAI-compatible | `snail-ai-models/snail-ai-model-embedding/snail-ai-model-embedding-provider-openai-compatible` |
| Rerank 模型 | 已支持，Qwen/HTTP Rerank | `snail-ai-models/snail-ai-model-rerank/snail-ai-model-rerank-provider-qwen` |
| 图片输入识别 | 已支持，作为 CHAT 模型的 `vision` 能力 | `ConfigExtAttrsDTO.capabilities`、`AgentChatAttachmentService` |
| Image / Speech 类型 | 数据模型可表示；`IMAGE` 类型不等同于本次 CHAT 图片输入能力 | `snail-ai-commons/snail-ai-commons-core` 中模型类型定义 |
| Claude / Gemini / Ollama / 火山引擎 | 可通过兼容接口或自定义 Provider 评估接入，不按当前源码描述为内置一等 Provider | 模型配置中的 `apiEndpoint` / `providerKey` |

## 模型管理三层结构

```text
智能体 / RAG / OpenAPI
        ↓
模型配置：apiKey、apiEndpoint、modelKey、模型类型、作用域
        ↓
模型提供商：名称、providerKey、图标、启用状态
```

| 层级 | 职责 | 说明 |
|------|------|------|
| 模型提供商 | 接入管理 | 定义厂商或服务端点信息 |
| 模型配置 | 参数管理 | 为具体模型配置密钥、端点、模型标识、适配器和扩展参数 |
| 业务消费 | 按类型选用 | 智能体使用 Chat，RAG 使用 Embedding/Rerank |

## 模型能力类型

| 类型 | 枚举值 | 当前用途 | 支持状态 |
|------|--------|----------|----------|
| 对话模型 | `CHAT` | 智能体对话、工具调用、流式输出；开启 `vision` 后支持图片输入识别 | 已支持 |
| 向量模型 | `EMBEDDING` | RAG 文档向量化、语义检索 | 已支持 |
| 重排序模型 | `RERANKER` | RAG 检索结果重排 | 已支持 |
| 图像模型 | `IMAGE` | 图像生成/理解 | 当前不作为已落地调用能力 |
| 语音模型 | `SPEECH` | 语音识别/合成 | 当前不作为已落地调用能力 |

## 图片输入识别

模型配置中的“支持图片输入”开关用于把 CHAT 模型标记为视觉模型。开启后，保存到 `configJson.capabilities` 的值会包含 `vision`，智能体预览与对话接口才允许随消息携带图片附件。

典型配置：

```json
{
  "modelType": "CHAT",
  "adapterKey": "openai-compatible",
  "configJson": {
    "capabilities": ["vision"]
  }
}
```

如果该模型还要作为 RAG Docling 图片 OCR 的视觉兜底模型，可同时开启“作为 OCR 视觉兜底模型”，对应 `configJson.defaultVisionModel=true`。普通图片对话只需要 `vision` 能力，不要求设置默认视觉模型。

## OpenAI-compatible 接入方式

对于 DeepSeek、通义千问兼容接口、Moonshot 等兼容 OpenAI API 协议的服务，可以使用 OpenAI-compatible 配置：

| 字段 | 示例 | 说明 |
|------|------|------|
| `providerKey` | `openai` 或自定义 key | 提供商标识 |
| `apiEndpoint` | `https://api.deepseek.com` | 服务端点 |
| `modelKey` | `deepseek-chat` | 实际模型名称 |
| `modelType` | `CHAT` | 模型能力类型 |

## 默认模型

系统支持为同一类型配置默认模型：

- 创建智能体时使用默认 `CHAT` 模型。
- 创建知识库时使用默认 `EMBEDDING` 模型。
- 启用重排时选择 `RERANKER` 模型。

## 功能导航

| 功能 | 说明 | 文档链接 |
|------|------|----------|
| 模型提供商管理 | 接入和管理 AI 服务端点 | [模型提供商](./provider) |
| 模型配置管理 | 创建、验证和维护具体模型参数 | [模型配置](./config) |
| 使用统计 | 查看模型调用量和 Token 消耗 | [使用统计](./usage-stats) |

## 相关源码

- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/AiModelController.java`
- `snail-ai-models/snail-ai-model-chat/snail-ai-model-chat-provider-openai-compatible`
- `snail-ai-models/snail-ai-model-embedding/snail-ai-model-embedding-provider-openai-compatible`
- `snail-ai-models/snail-ai-model-rerank/snail-ai-model-rerank-provider-qwen`
