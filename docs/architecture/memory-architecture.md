# 记忆架构

## 概述

Snail AI 的记忆系统为智能体提供**跨对话的信息持久化能力**，使 Agent 能够"记住"用户的偏好、历史决策和关键信息。记忆系统分为 **短期记忆** 和 **长期记忆** 两个层面，通过不同的机制协同工作，在保持上下文连贯性的同时控制 Token 消耗。

```mermaid
graph TB
    subgraph ShortTerm["短期记忆"]
        direction TB
        SW["滑动窗口<br/>最近 N 轮对话"]
        SW_DESC["机制：保留最近 N 条消息<br/>超出自动截断<br/>默认：20 条"]
    end

    subgraph LongTerm["长期记忆"]
        direction TB
        EXT["记忆抽取<br/>LLM 提取结构化记忆"]
        STORE["向量化存储<br/>Embedding → Vector Store"]
        RECALL["语义召回<br/>查询相关记忆注入上下文"]
    end

    subgraph Agent["智能体对话"]
        direction TB
        CTX["完整上下文 =<br/>System Prompt<br/>+ 长期记忆<br/>+ 短期记忆<br/>+ RAG 知识<br/>+ 当前消息"]
    end

    SW --> CTX
    RECALL --> CTX
    EXT --> STORE
    STORE --> RECALL

    style ShortTerm fill:#3498db,color:#fff
    style LongTerm fill:#9b59b6,color:#fff
    style Agent fill:#2ecc71,color:#fff
```

## 短期记忆：滑动窗口

短期记忆采用**滑动窗口（Sliding Window）** 机制，保留当前会话中最近的 N 条消息。当消息数量超过窗口大小时，最早的消息被自动截断。

```mermaid
graph LR
    subgraph Window["滑动窗口 (shortTermMemorySize=20)"]
        direction TB
        M1["msg 1 (oldest)"]
        M2["msg 2"]
        M3["..."]
        M19["msg 19"]
        M20["msg 20 (newest)"]
    end

    NEW["新消息 msg 21"] --> Window
    M1 -.->|"被截断"| OUT["丢弃"]

    style Window fill:#3498db,color:#fff
    style OUT fill:#e74c3c,color:#fff
    style NEW fill:#2ecc71,color:#fff
```

### 配置参数

| 参数 | 位置 | 说明 | 默认值 |
|------|------|------|--------|
| `shortTermMemorySize` | Agent 配置 | 滑动窗口保留的消息条数 | 20 |

### 工作原理

```mermaid
sequenceDiagram
    participant User as 用户
    participant Handler as ContextCollectorHandler
    participant DB as 数据库

    User->>Handler: 发送消息
    Handler->>DB: 查询会话历史记录<br/>ORDER BY createDt DESC<br/>LIMIT shortTermMemorySize
    DB-->>Handler: 返回最近 20 条消息
    Handler->>Handler: 按时间正序排列
    Handler->>Handler: 添加到 ChatContext.messages
    Note over Handler: messages = [历史消息...] + [当前消息]
```

**设计考量：**

- **为什么不保留全部历史？** 大模型的上下文窗口有限（4K-128K Token），且 Token 计费与长度正相关。滑动窗口在保持上下文连贯性和控制成本之间取得平衡。
- **窗口大小建议：** 一般对话场景 20 条即可；需要长上下文的分析场景可调大至 50-100 条。

## 长期记忆：向量化召回

长期记忆通过 **LLM 抽取 → 向量化存储 → 语义召回** 三个阶段实现跨会话的信息持久化。

### 记忆抽取

在对话完成后，系统会异步调用 LLM 从对话内容中提取结构化的记忆条目：

```mermaid
flowchart TD
    A["对话完成<br/>(ChatStreamObserver.onCompleted)"] --> B["异步触发记忆抽取"]
    B --> C["收集最近对话内容"]
    C --> D["调用 LLM<br/>（extractionModelId 指定的模型）"]
    D --> E["LLM 输出结构化记忆"]
    E --> F["解析记忆条目"]
    F --> G["每条记忆包含：<br/>type, title, content,<br/>tags, relevanceScore,<br/>confidenceScore"]
    G --> H["向量化<br/>Embedding Model"]
    H --> I["存入向量数据库"]
    I --> J["创建 Memory 记录"]

    style A fill:#3498db,color:#fff
    style J fill:#2ecc71,color:#fff
```

**抽取配置（MemoryConfig）：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `extractionModelId` | 用于抽取记忆的对话模型 ID | Agent 绑定的模型 |
| `extractionInterval` | 抽取间隔（每 N 轮对话抽取一次） | 5 |
| `maxMemoriesPerExtraction` | 单次抽取的最大记忆条数 | 10 |
| `customExtractionPrompt` | 自定义抽取提示词 | 系统默认 |
| `memoryExpirationDays` | 记忆过期天数 | 无限 |

### 记忆类型

Snail AI 定义了 **5 种记忆类型**，覆盖不同维度的用户信息：

```mermaid
mindmap
  root((记忆类型))
    FACT
      用户的客观事实
      "我在北京工作"
      "公司使用 Java 技术栈"
    DECISION
      用户的决策记录
      "选择了方案 A"
      "决定使用 PgVector"
    PREFERENCE
      用户的偏好设置
      "喜欢简洁的回答风格"
      "偏好中文回复"
    TASK_PROGRESS
      任务执行进度
      "报表已完成 50%"
      "待办：优化检索"
    REFERENCE
      参考资料引用
      "参考了 Spring AI 文档"
      "使用的模型版本是 GPT-4"
```

| 类型 | 标识 | 说明 | 典型示例 |
|------|------|------|----------|
| **事实** | `FACT` | 用户陈述的客观事实 | "我们部门有 30 人" |
| **决策** | `DECISION` | 用户做出的决策或结论 | "我们决定使用微服务架构" |
| **偏好** | `PREFERENCE` | 用户表达的喜好和倾向 | "请用表格形式回答" |
| **任务进度** | `TASK_PROGRESS` | 正在进行的任务状态 | "数据迁移已完成 80%" |
| **参考资料** | `REFERENCE` | 提及的文档、链接等引用 | "参考了 RFC 7231 规范" |

### 记忆生命周期状态机

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: 记忆创建

    ACTIVE --> ARCHIVED: 归档操作
    ACTIVE --> SUPPRESSED: 压制操作
    ARCHIVED --> ACTIVE: 激活操作
    SUPPRESSED --> ACTIVE: 激活操作

    ACTIVE --> [*]: 删除

    note right of ACTIVE
        激活状态
        可被语义检索召回
        正常参与记忆注入
    end note

    note right of ARCHIVED
        归档状态
        不参与自动召回
        可手动查看和恢复
    end note

    note right of SUPPRESSED
        压制状态
        不参与自动召回
        用于标记不准确的记忆
        可手动恢复
    end note
```

**状态转换 API：**

| 操作 | API 端点 | 说明 |
|------|----------|------|
| 归档 | `POST /memory/{id}/archive` | ACTIVE → ARCHIVED |
| 压制 | `POST /memory/{id}/suppress` | ACTIVE → SUPPRESSED |
| 激活 | `POST /memory/{id}/activate` | ARCHIVED/SUPPRESSED → ACTIVE |
| 删除 | `DELETE /memory/{id}` | 永久删除 |

### 记忆数据模型

```mermaid
classDiagram
    class ConversationMemory {
        +Long id
        +Long agentId
        +Long userId
        +String conversationId
        +Long sourceMessageId
        +MemoryType memoryType
        +String category
        +String title
        +String content
        +String[] tags
        +Long vectorStoreInstanceId
        +String vectorId
        +Float relevanceScore
        +Float confidenceScore
        +MemoryStatus status
        +Integer accessCount
        +DateTime accessedAt
        +DateTime createDt
        +DateTime updateDt
    }

    class MemoryType {
        <<enumeration>>
        FACT
        DECISION
        PREFERENCE
        TASK_PROGRESS
        REFERENCE
    }

    class MemoryStatus {
        <<enumeration>>
        ACTIVE
        ARCHIVED
        SUPPRESSED
    }

    ConversationMemory --> MemoryType
    ConversationMemory --> MemoryStatus
```

## 记忆注入：MemoryInjectionAdvisor

`MemoryInjectionAdvisor` 负责在对话请求处理过程中，将相关的长期记忆检索出来并注入到 Agent 的上下文中。

```mermaid
sequenceDiagram
    participant Handler as ContextCollectorHandler
    participant Advisor as MemoryInjectionAdvisor
    participant Config as MemoryConfig
    participant Embed as Embedding Model
    participant VStore as Vector Store
    participant Reranker as Reranker Model

    Handler->>Advisor: 注入记忆
    Advisor->>Config: 读取记忆库配置

    Note over Advisor: 1. Query 改写 (可选)
    alt rewriteEnabled = true
        Advisor->>Advisor: LLM 改写用户查询
    end

    Note over Advisor: 2. 向量检索
    Advisor->>Embed: 嵌入查询向量
    Embed-->>Advisor: 查询向量
    Advisor->>VStore: 相似度搜索<br/>maxRecall 条
    VStore-->>Advisor: 候选记忆列表

    Note over Advisor: 3. BM25 检索 (可选)
    alt searchEngineEnable = true
        Advisor->>Advisor: BM25 关键词检索
        Advisor->>Advisor: 融合两路结果
    end

    Note over Advisor: 4. 重排序 (可选)
    alt rerankEnabled = true
        Advisor->>Reranker: 重排候选记忆
        Reranker-->>Advisor: 重排结果
    end

    Note over Advisor: 5. 阈值过滤
    alt thresholdEnabled = true
        Advisor->>Advisor: 过滤低于 similarityThreshold 的记忆
    end

    Note over Advisor: 6. 注入上下文
    Advisor->>Advisor: 构建记忆上下文文本
    Advisor-->>Handler: 注入到 System Prompt
```

### 记忆注入格式

记忆被注入到系统提示词中，格式如下：

```
## 用户历史记忆

以下是该用户的历史记忆信息，请在回答时参考：

- [事实] 用户在北京的互联网公司工作，团队使用 Java 技术栈
- [偏好] 用户喜欢简洁清晰的回答，偏好使用代码示例说明
- [决策] 用户决定项目使用 Spring Boot 4 + Spring AI 2.0
- [任务进度] 数据库迁移项目已完成 PostgreSQL 部分，下一步是达梦适配
```

## 记忆库配置（MemoryConfig）

每个智能体可以绑定一个独立的**记忆库配置**，包含记忆检索和抽取的全部参数：

```mermaid
graph TB
    subgraph MemConfig["MemoryConfig 记忆库配置"]
        direction TB

        subgraph Storage["存储配置"]
            VS["vectorStoreInstanceId<br/>向量存储实例"]
            EM["embeddingModelId<br/>嵌入模型"]
            DIM["dimensionOfVectorModel<br/>向量维度"]
        end

        subgraph Search["检索配置"]
            MR["maxRecall: 最大召回数"]
            RW["rewriteEnabled: 查询改写"]
            RE["rerankEnabled: 重排序"]
            RM["rerankModelId: Reranker 模型"]
            ERC["enterRerankCount: 进入重排数量"]
            TE["thresholdEnabled: 阈值过滤"]
            ST["similarityThreshold: 相似度阈值"]
            FS["fusionStrategy: 融合策略"]
            DW["denseWeight: 向量权重"]
            RK["rrfK: RRF 常数"]
        end

        subgraph Extraction["抽取配置"]
            EI["extractionInterval: 抽取间隔"]
            ME["maxMemoriesPerExtraction: 单次最大条数"]
            XM["extractionModelId: 抽取模型"]
            CP["customExtractionPrompt: 自定义提示词"]
            ED["memoryExpirationDays: 过期天数"]
        end

        subgraph Engine["搜索引擎 (可选)"]
            SEE["searchEngineEnable: 启用搜索引擎"]
            SEI["searchEngineInstanceId: 引擎实例"]
        end
    end

    style MemConfig fill:#ecf0f1,color:#2c3e50
    style Storage fill:#3498db,color:#fff
    style Search fill:#9b59b6,color:#fff
    style Extraction fill:#e74c3c,color:#fff
    style Engine fill:#f39c12,color:#fff
```

### Agent 与 MemoryConfig 的关系

```mermaid
graph LR
    A1["Agent 1<br/>客服助手"] -->|memoryConfigId=1| MC1["MemoryConfig 1<br/>通用记忆库"]
    A2["Agent 2<br/>技术顾问"] -->|memoryConfigId=1| MC1
    A3["Agent 3<br/>数据分析"] -->|memoryConfigId=2| MC2["MemoryConfig 2<br/>分析记忆库"]
    A4["Agent 4<br/>简单问答"] -->|memoryEnabled=false| NONE["不启用记忆"]

    MC1 --> VS1["PgVector 实例"]
    MC2 --> VS2["Milvus 实例"]

    style MC1 fill:#9b59b6,color:#fff
    style MC2 fill:#e74c3c,color:#fff
```

**设计说明：** 记忆库配置与智能体是**多对一**关系——多个智能体可以共享同一个记忆库配置，但每个智能体只能绑定一个记忆库。记忆数据本身按 `agentId + userId` 进行隔离，即使共享记忆库配置，不同智能体/用户的记忆也互不干扰。

## 存储后端

### 记忆存储流程

```mermaid
flowchart TD
    A["LLM 抽取出记忆"] --> B["构建 ConversationMemory 对象"]
    B --> C["Embedding Model 向量化"]
    C --> D["存入向量数据库"]
    D --> E["获取 vectorId"]
    E --> F["保存到关系数据库<br/>（含 vectorId 引用）"]

    style A fill:#3498db,color:#fff
    style F fill:#2ecc71,color:#fff
```

**双重存储：**

- **关系数据库** -- 存储记忆的结构化元数据（类型、标题、内容、状态、标签等）
- **向量数据库** -- 存储记忆内容的向量表示，用于语义检索

### 记忆检索流程

```mermaid
flowchart TD
    A["用户发送消息"] --> B["提取检索 Query"]
    B --> C["向量化 Query"]
    C --> D["向量数据库相似度搜索"]
    D --> E["获取候选 vectorId 列表"]
    E --> F["从关系数据库加载完整记忆"]
    F --> G["过滤：status=ACTIVE"]
    G --> H["过滤：同一 agentId + userId"]
    H --> I["（可选）Reranker 精排"]
    I --> J["（可选）阈值过滤"]
    J --> K["注入到 Agent 上下文"]

    style A fill:#3498db,color:#fff
    style K fill:#2ecc71,color:#fff
```

## 记忆统计与调试

### 统计指标

```mermaid
graph TB
    subgraph Stats["记忆统计 (MemoryStats)"]
        direction TB
        S1["totalMemories: 总记忆数"]
        S2["byType: 按类型分布<br/>FACT: 120, DECISION: 45, ..."]
        S3["mostUsed: 最常使用的记忆 ID"]
        S4["retrievalEffectiveness: 召回有效率"]
    end

    style Stats fill:#ecf0f1,color:#2c3e50
```

### 上下文预览

`ContextPreview` API 允许调试完整的 Agent 上下文组装结果：

```mermaid
flowchart LR
    A["GET /memory/agent/{agentId}/conversations/{conversationId}/context-preview"] --> B["返回 ContextPreview"]
    B --> C["messages: 完整消息列表"]
    B --> D["memories: 召回的记忆列表"]
    B --> E["estimatedTokens: 预估 Token 数"]
    B --> F["compressionApplied: 是否压缩"]
```

### 记忆调试

```mermaid
sequenceDiagram
    participant Admin as 管理员
    participant API as Memory Config API
    participant Config as MemoryConfig
    participant VStore as Vector Store

    Admin->>API: POST /memory/config/{id}/debug<br/>{agentId, userId, query}
    API->>Config: 读取检索配置
    API->>VStore: 执行检索流水线
    VStore-->>API: 返回召回结果
    API-->>Admin: 返回 ConversationMemory[]<br/>（含分数和排名）
```

管理员可以使用 Debug API 测试记忆检索的效果，模拟特定用户的查询，查看召回的记忆列表及其相关性分数，用于调优检索参数。

## 端到端流程总结

```mermaid
graph TB
    subgraph 对话阶段["对话阶段"]
        U["用户发送消息"]
        U --> STM["加载短期记忆<br/>（最近 20 条消息）"]
        STM --> LTM["检索长期记忆<br/>（向量语义召回）"]
        LTM --> CTX["组装上下文"]
        CTX --> LLM["大模型调用"]
        LLM --> RESP["返回回答"]
    end

    subgraph 抽取阶段["对话后异步抽取"]
        RESP --> EXTRACT["LLM 记忆抽取"]
        EXTRACT --> PARSE["解析结构化记忆"]
        PARSE --> EMBED["向量化存储"]
        EMBED --> SAVE["持久化到数据库"]
    end

    subgraph 下次对话["下次对话"]
        SAVE -.->|"已存入记忆库"| LTM2["检索长期记忆"]
        LTM2 --> CTX2["注入到新对话上下文"]
    end

    style 对话阶段 fill:#3498db,color:#fff
    style 抽取阶段 fill:#9b59b6,color:#fff
    style 下次对话 fill:#2ecc71,color:#fff
```
