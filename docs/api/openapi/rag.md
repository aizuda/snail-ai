# 知识库查询

当前版本没有提供独立的 `/openapi/v1/rag/**` 外部接口。第三方系统需要使用知识库能力时，应先在管理后台把 RAG 知识库绑定到智能体，再通过 OpenAPI 智能体对话接口调用。

## 当前支持状态

| 能力 | 状态 | 使用方式 |
|------|------|----------|
| 管理后台知识库管理 | 已支持 | 使用 Admin API 或管理后台的 RAG 页面 |
| 智能体对话中检索知识库 | 已支持 | 将知识库绑定到智能体后调用 `/openapi/v1/agent/chat` |
| 独立 OpenAPI RAG 搜索 | 当前未提供 | 如需开放独立搜索接口，需要新增 OpenAPI Controller |
| 独立 OpenAPI RAG 问答 | 当前未提供 | 使用绑定知识库的智能体对话替代 |

## 推荐调用方式

1. 在管理后台创建知识库并上传文档。
2. 等待文档解析、分片和向量化完成。
3. 在智能体配置中绑定该知识库。
4. 使用 OpenAPI 对话接口访问该智能体。

```bash
curl -N -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>' \
  -d '{
    "agentId": 1,
    "openId": "external-user-001",
    "content": "请根据知识库说明退款政策"
  }'
```

## 相关源码

- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/RagSearchController.java`
- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/RagDocumentController.java`
- `snail-ai-server/snail-ai-server-admin/src/main/java/com/aizuda/snail/ai/admin/controller/RagChunkController.java`
- `snail-ai-server/snail-ai-server-openapi/src/main/java/com/aizuda/snail/ai/openapi/controller/OpenApiChatController.java`
