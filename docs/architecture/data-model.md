# 数据模型

## 概述

Snail AI 的数据模型围绕 **Agent（智能体）** 这一核心实体展开，通过关联关系连接对话、知识库、模型、工具、技能、记忆等子系统。本文档使用 ER 图展示核心实体及其关系，并说明数据库设计规范。

## 核心 ER 图

```mermaid
erDiagram
    User ||--o{ Agent : "创建"
    User ||--o{ Conversation : "发起"
    User ||--o{ ConversationMemory : "拥有"

    Agent ||--o{ Conversation : "关联"
    Agent ||--o{ AgentConfig : "配置"
    Agent }o--o| App : "绑定执行应用"
    Agent }o--o| Rag : "绑定知识库"
    Agent }o--o| MemoryConfig : "绑定记忆库"
    Agent }o--o{ McpServer : "绑定 MCP"
    Agent }o--o{ Skill : "绑定技能"
    Agent }o--o| ModelConfig : "绑定对话模型"

    Conversation ||--o{ ConversationRecord : "包含消息"

    Rag ||--o{ RagDocument : "包含文档"
    RagDocument ||--o{ RagChunk : "包含分片"
    Rag }o--o| StoreInstance : "向量存储"
    Rag }o--o| StoreInstance : "搜索引擎"

    ModelProvider ||--o{ ModelConfig : "提供模型"

    MemoryConfig }o--o| StoreInstance : "向量存储"
    ConversationMemory }o--o| MemoryConfig : "所属记忆库"

    App ||--o{ ClientNode : "管理节点"

    Trace ||--o{ Observation : "包含观测"
    Trace ||--o{ Score : "关联评分"

    Skill ||--o{ SkillFile : "包含文件"

    User {
        bigint id PK
        varchar username
        varchar email
        varchar password
        int role
        bigint tokens
        datetime expireDt
        bigint totals
        datetime createDt
        datetime updateDt
    }

    Agent {
        bigint id PK
        varchar name
        varchar description
        varchar avatar
        text instruction
        text greeting
        json presetQuestions
        bigint chatModelId FK
        boolean mcpEnabled
        boolean skillEnabled
        boolean webSearchEnabled
        boolean ragEnabled
        boolean memoryEnabled
        int shortTermMemorySize
        bigint ragId FK
        bigint memoryConfigId FK
        varchar appId FK
        int status
        boolean isFeatured
        bigint creator FK
        datetime createDt
        datetime updateDt
    }

    AgentConfig {
        bigint id PK
        bigint agentId FK
        varchar configKey
        text configValue
        datetime createDt
    }

    Conversation {
        bigint id PK
        varchar conversationId UK
        bigint agentId FK
        bigint userId FK
        varchar title
        datetime createDt
        datetime updateDt
    }

    ConversationRecord {
        bigint id PK
        varchar conversationId FK
        varchar question
        text answer
        int status
        datetime createDt
    }
```

## 知识库相关实体

```mermaid
erDiagram
    Rag ||--o{ RagDocument : "包含"
    RagDocument ||--o{ RagChunk : "分片"
    Rag }o--o| StoreInstance : "向量存储"
    Rag }o--o| StoreInstance : "搜索引擎"

    Rag {
        bigint id PK
        varchar name
        varchar description
        varchar icon
        bigint vectorStoreInstanceId FK
        int dimensionOfVectorModel
        bigint embeddingModelId FK
        varchar embeddingModelName
        bigint rerankModelId FK
        boolean searchEngineEnable
        bigint searchEngineInstanceId FK
        varchar delimiter
        json knowledgeEnhancement
        int documentCount
        int chunkCount
        varchar status
        json config
        int dedupStrategy
        int dedupAction
        boolean uploadConfirm
        datetime createdDt
        datetime updateDt
    }

    RagDocument {
        bigint id PK
        bigint ragId FK
        varchar name
        varchar fileType
        varchar sourceType
        varchar sourcePath
        varchar previewUrl
        varchar downloadUrl
        int status
        int progress
        int chunkCount
        varchar fileSize
        varchar errorMsg
        bigint resourceId FK
        datetime createdAt
    }

    RagChunk {
        bigint id PK
        bigint ragId FK
        bigint documentId FK
        varchar documentName
        int paragraphIndex
        int chunkIndex
        text content
        int tokenCount
        varchar vectorId
        varchar vectorStatus
        varchar type
        varchar imageUrl
        datetime createdDt
        datetime updateDt
    }

    StoreInstance {
        bigint id PK
        varchar name
        int category
        int type
        json config
        int status
        boolean isDefault
        datetime createDt
        datetime updateDt
    }
```

## 模型相关实体

```mermaid
erDiagram
    ModelProvider ||--o{ ModelConfig : "提供"

    ModelProvider {
        bigint id PK
        varchar providerName
        varchar providerKey UK
        varchar description
        varchar iconUrl
        boolean isEnabled
        datetime createdDt
        datetime updatedDt
    }

    ModelConfig {
        bigint id PK
        bigint providerId FK
        varchar modelName
        varchar modelKey
        varchar modelType
        varchar description
        varchar apiKey
        varchar apiEndpoint
        json configJson
        bigint ownerId FK
        varchar scope
        boolean isDefault
        boolean isEnabled
        datetime createdDt
        datetime updatedDt
    }
```

**模型类型枚举（ModelType）：**

| 值 | 说明 | 用途 |
|----|------|------|
| `CHAT` | 对话模型 | Agent 对话、记忆抽取、Query 改写 |
| `EMBEDDING` | 嵌入模型 | RAG 向量化、记忆向量化 |
| `RERANKER` | 重排序模型 | RAG 检索精排、记忆检索精排 |
| `IMAGE` | 图像模型 | 图像生成 |
| `SPEECH` | 语音模型 | 语音合成/识别 |

**模型作用域（ModelScope）：**

| 值 | 说明 |
|----|------|
| `GLOBAL` | 全局模型，所有用户可用 |
| `PERSONAL` | 个人模型，仅创建者可用 |

## MCP 与技能实体

```mermaid
erDiagram
    McpServer {
        bigint id PK
        varchar name
        varchar description
        int transportType
        varchar baseUri
        varchar endpoint
        varchar command
        json args
        json envVars
        varchar version
        int authType
        json authConfig
        varchar status
        json capabilities
        datetime lastConnectDt
        datetime createDt
        datetime updateDt
    }

    Skill ||--o{ SkillFile : "包含文件"

    Skill {
        bigint id PK
        varchar name
        varchar description
        varchar fileName
        bigint fileSize
        datetime createDt
        datetime updateDt
    }

    SkillFile {
        bigint id PK
        bigint skillId FK
        varchar path
        varchar type
        text content
        bigint size
        datetime createDt
    }
```

**MCP 传输类型（TransportType）：**

| 值 | 说明 |
|----|------|
| `1` | SSE (Server-Sent Events) |
| `2` | Streamable HTTP |
| `3` | Stdio（本地进程） |

**MCP 认证类型（AuthType）：**

| 值 | 说明 |
|----|------|
| `0` | 无认证 |
| `1` | API Key |
| `2` | Bearer Token |
| `3` | OAuth 2.0 |

## 记忆相关实体

```mermaid
erDiagram
    MemoryConfig ||--o{ ConversationMemory : "管理"
    MemoryConfig }o--o| StoreInstance : "向量存储"

    MemoryConfig {
        bigint id PK
        varchar name
        int status
        bigint vectorStoreInstanceId FK
        bigint embeddingModelId FK
        int dimensionOfVectorModel
        boolean searchEngineEnable
        bigint searchEngineInstanceId FK
        int maxRecall
        boolean rewriteEnabled
        boolean rerankEnabled
        bigint rerankModelId FK
        int enterRerankCount
        boolean thresholdEnabled
        float similarityThreshold
        varchar fusionStrategy
        float denseWeight
        int rrfK
        int extractionInterval
        int maxMemoriesPerExtraction
        bigint extractionModelId FK
        text customExtractionPrompt
        int memoryExpirationDays
        datetime createDt
        datetime updateDt
    }

    ConversationMemory {
        bigint id PK
        bigint agentId FK
        bigint userId FK
        varchar conversationId FK
        bigint sourceMessageId FK
        varchar memoryType
        varchar category
        varchar title
        text content
        json tags
        bigint vectorStoreInstanceId FK
        varchar vectorId
        float relevanceScore
        float confidenceScore
        varchar status
        int accessCount
        datetime accessedAt
        datetime createDt
        datetime updateDt
    }
```

## 应用与分布式实体

```mermaid
erDiagram
    App ||--o{ ClientNode : "包含节点"

    App {
        bigint id PK
        varchar appId UK
        varchar appName
        varchar description
        varchar token
        varchar routeStrategy
        int status
        datetime createDt
    }

    ClientNode {
        bigint id PK
        varchar appId FK
        varchar appName
        varchar hostId
        varchar hostIp
        int grpcPort
        int maxConcurrent
        int activeChats
        json labels
        datetime expireDt
        boolean online
    }
```

## 追踪与可观测性实体

```mermaid
erDiagram
    Trace ||--o{ Observation : "包含"
    Trace ||--o{ Score : "评分"

    Trace {
        varchar id PK
        bigint agentId FK
        varchar conversationId FK
        bigint userId FK
        text input
        text output
        varchar model
        bigint startTime
        bigint endTime
        bigint durationMs
        int status
        varchar statusMessage
        varchar environment
        varchar release
        boolean bookmarked
        json tags
        bigint totalInputTokens
        bigint totalOutputTokens
        decimal totalCost
    }

    Observation {
        varchar id PK
        varchar traceId FK
        varchar parentObservationId FK
        varchar type
        varchar name
        text input
        text output
        bigint startTime
        bigint endTime
        bigint durationMs
        bigint completionStartTime
        varchar model
        json modelParameters
        json usageDetails
        json costDetails
        decimal totalCost
        varchar finishReason
        text thinkingContent
        json toolDefinitions
        json toolCalls
        json toolCallNames
        varchar toolCallId
        varchar level
        int status
        varchar statusMessage
    }

    Score {
        varchar id PK
        varchar traceId FK
        varchar observationId FK
        varchar name
        decimal value
        varchar stringValue
        varchar dataType
        varchar source
        varchar comment
        varchar authorUserName
    }
```

**Observation 类型（ObservationType）：**

| 类型 | 说明 |
|------|------|
| `GENERATION` | 大模型调用（含输入/输出 Token、费用） |
| `TOOL` | 工具调用（MCP 工具执行） |
| `THINKING` | 思考过程（推理模型的 chain-of-thought） |
| `SPAN` | 通用执行段（如责任链某个 Handler） |
| `EVENT` | 事件节点 |
| `AGENT` | Agent 级别的观测 |
| `RETRIEVER` | RAG 检索 |
| `EMBEDDING` | 向量嵌入 |

**Score 数据类型：**

| 类型 | 说明 |
|------|------|
| `NUMERIC` | 数值型（如 1-5 星评分） |
| `CATEGORICAL` | 分类型（如 "positive" / "negative"） |
| `BOOLEAN` | 布尔型（如 "有帮助" / "无帮助"） |
| `TEXT` | 文本型（自由评论） |

## 资源管理实体

```mermaid
erDiagram
    Resource {
        bigint id PK
        varchar storageKey
        varchar originalName
        bigint fileSize
        varchar mimeType
        varchar storageType
        varchar accessUrl
        varchar bizType
        bigint bizId FK
        bigint creatorId FK
        datetime createDt
    }
```

**bizType 业务类型：**

| 值 | 说明 |
|----|------|
| `AGENT_AVATAR` | 智能体头像 |
| `RAG_DOCUMENT` | RAG 文档 |
| `SKILL_FILE` | 技能文件 |
| `CHAT_IMAGE` | 对话中的图片 |

## 全局 ER 关系总览

```mermaid
graph TB
    subgraph Core["核心实体"]
        USER["User<br/>用户"]
        AGENT["Agent<br/>智能体"]
        CONV["Conversation<br/>会话"]
        RECORD["ConversationRecord<br/>消息记录"]
    end

    subgraph Knowledge["知识系统"]
        RAG["Rag<br/>知识库"]
        DOC["RagDocument<br/>文档"]
        CHUNK["RagChunk<br/>分片"]
    end

    subgraph Model["模型系统"]
        PROVIDER["ModelProvider<br/>模型供应商"]
        CONFIG["ModelConfig<br/>模型配置"]
    end

    subgraph Tool["工具系统"]
        MCP["McpServer<br/>MCP 服务"]
        SKILL["Skill<br/>技能"]
    end

    subgraph Memory["记忆系统"]
        MC["MemoryConfig<br/>记忆库配置"]
        MEM["ConversationMemory<br/>记忆"]
    end

    subgraph Infra["基础设施"]
        APP["App<br/>应用"]
        NODE["ClientNode<br/>客户端节点"]
        STORE["StoreInstance<br/>存储实例"]
        RES["Resource<br/>资源"]
    end

    subgraph Observe["可观测性"]
        TRACE["Trace<br/>追踪"]
        OBS["Observation<br/>观测"]
        SCORE["Score<br/>评分"]
    end

    USER --> AGENT
    USER --> CONV
    AGENT --> CONV
    CONV --> RECORD
    AGENT --> RAG
    AGENT --> MCP
    AGENT --> SKILL
    AGENT --> MC
    AGENT --> CONFIG
    AGENT --> APP
    RAG --> DOC
    DOC --> CHUNK
    RAG --> STORE
    MC --> STORE
    PROVIDER --> CONFIG
    APP --> NODE
    TRACE --> OBS
    TRACE --> SCORE
    AGENT --> TRACE
    USER --> MEM

    style Core fill:#3498db,color:#fff
    style Knowledge fill:#2ecc71,color:#fff
    style Model fill:#9b59b6,color:#fff
    style Tool fill:#e74c3c,color:#fff
    style Memory fill:#f39c12,color:#fff
    style Infra fill:#1abc9c,color:#fff
    style Observe fill:#e67e22,color:#fff
```

## 数据库设计规范

### 表命名约定

| 前缀 | 所属模块 | 示例 |
|------|----------|------|
| `t_agent_` | 智能体 | `t_agent_info`, `t_agent_config` |
| `t_conversation_` | 对话 | `t_conversation_info`, `t_conversation_record` |
| `t_rag_` | 知识库 | `t_rag_info`, `t_rag_document`, `t_rag_chunk` |
| `t_model_` | 模型 | `t_model_provider`, `t_model_config` |
| `t_mcp_` | MCP | `t_mcp_server` |
| `t_skill_` | 技能 | `t_skill_info`, `t_skill_file` |
| `t_memory_` | 记忆 | `t_memory_info`, `t_memory_config` |
| `t_app_` | 应用 | `t_app_info`, `t_app_client_node` |
| `t_trace_` | 追踪 | `t_trace_info`, `t_trace_observation`, `t_trace_score` |
| `t_resource_` | 资源 | `t_resource_info` |
| `t_user_` | 用户 | `t_user_info` |
| `t_store_` | 存储 | `t_store_instance` |

### 通用字段规范

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT AUTO_INCREMENT | 主键 |
| `create_dt` | DATETIME | 创建时间 |
| `update_dt` | DATETIME | 更新时间 |
| `is_deleted` | TINYINT(1) | 逻辑删除标记（0=未删除, 1=已删除） |


