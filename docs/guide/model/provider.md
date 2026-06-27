# 模型提供商

模型提供商（Provider）用于描述一个 AI 服务端点或厂商。当前版本推荐使用 OpenAI-compatible Provider 接入 Chat/Embedding 模型，Rerank 使用 Qwen/HTTP Rerank 实现。

## 当前支持状态

| Provider/能力 | 状态 | 说明 |
|---------------|------|------|
| OpenAI-compatible Chat | 已支持 | 通过 OpenAI 兼容协议创建 Chat 模型 |
| OpenAI-compatible Embedding | 已支持 | 通过 OpenAI 兼容协议创建 Embedding 模型 |
| Qwen/HTTP Rerank | 已支持 | 用于 RAG 检索结果重排序 |
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
3. 设置为默认 Chat 模型。
4. 创建智能体并发起对话验证。

## 注意事项

- 不要仅因为文档中出现某个厂商名称，就默认它是源码内置 Provider。
- 只有 Chat、Embedding、Rerank 是当前源码已落地的调用规格。
- 接入新的非兼容协议时，需要新增对应模型 Provider 实现，而不只是新增 Provider 元数据。

## 相关源码

- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/AiModelController.java`
- `snail-ai-models/snail-ai-model-chat/snail-ai-model-chat-provider-openai-compatible`
- `snail-ai-models/snail-ai-model-embedding/snail-ai-model-embedding-provider-openai-compatible`
- `snail-ai-models/snail-ai-model-rerank/snail-ai-model-rerank-provider-qwen`
