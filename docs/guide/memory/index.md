# 记忆系统

当前版本的记忆系统主要提供对话短期记忆能力，用于在多轮对话中保留最近上下文。文档中涉及长期记忆、向量化召回和记忆库管理的内容，应以源码实际实现为准逐步补充。

## 当前支持状态

| 能力 | 状态 | 来源 |
|------|------|------|
| 短期记忆滑动窗口 | 已支持 | `snail-ai-starter/src/main/resources/application.yml` 中 `snail-ai.memory.short-term` |
| 短期记忆内存存储 | 已支持 | `store-type: memory` |
| 短期记忆数据库存储 | 已支持配置项 | `store-type: db` |
| 对话记忆 PO/Mapper | 已存在 | `snail-ai-server/snail-ai-server-persistence` |
| 独立记忆管理 Admin API | 当前文档不作为已完整开放能力描述 | 未发现独立 `MemoryController` |
| 长期记忆向量召回 | 谨慎使用，需以当前源码实现验证 | 后续按实际实现补充 |

## 配置示例

```yaml
snail-ai:
  memory:
    short-term:
      # memory: JVM 内存，适合单机体验；db: 数据库存储，适合需要持久化的场景
      store-type: memory
```

## 使用建议

- 本地体验或单机部署优先使用 `memory`，配置简单、性能高。
- 多实例或需要重启后保留上下文时，再评估使用 `db`。
- 如果需要长期记忆或向量召回，请先检查当前源码和数据库表结构，不要仅根据旧文档假设功能完整可用。

## 与 RAG 的区别

| 能力 | 目标 | 数据来源 |
|------|------|----------|
| 短期记忆 | 保留最近对话上下文 | 当前用户的近期消息 |
| RAG 知识库 | 基于文档检索回答 | 用户上传的知识文档 |

## 相关源码

- `snail-ai-starter/src/main/resources/application.yml`
- `snail-ai-server/snail-ai-server-persistence`
- `snail-ai-agent/snail-ai-agent-executor`
