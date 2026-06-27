# RAG 流水线

## 概述

Snail AI 的 RAG（Retrieval-Augmented Generation，检索增强生成）流水线分为两大阶段：**文档摄入流水线（Ingestion Pipeline）** 和**检索流水线（Retrieval Pipeline）**。文档摄入负责将原始文档处理成可检索的知识片段并索引；检索流水线负责在对话时从知识库中召回相关知识并注入到 Agent 上下文中。

```mermaid
graph TB
    subgraph Ingestion["文档摄入流水线"]
        direction LR
        UP["文档上传"] --> DEDUP["去重检查"]
        DEDUP --> PARSE["格式解析"]
        PARSE --> CHUNK["文本分片"]
        CHUNK --> EMBED["向量嵌入"]
        EMBED --> INDEX["索引存储"]
    end

    subgraph Retrieval["检索流水线"]
        direction LR
        QUERY["用户查询"] --> REWRITE["Query 改写"]
        REWRITE --> SEARCH["并行检索"]
        SEARCH --> FUSION["结果融合"]
        FUSION --> RERANK["重排序"]
        RERANK --> FILTER["阈值过滤"]
        FILTER --> INJECT["上下文注入"]
    end

    INDEX -.->|"知识库"| SEARCH

    style Ingestion fill:#3498db,color:#fff
    style Retrieval fill:#2ecc71,color:#fff
```

## 文档摄入流水线

### 完整流程

```mermaid
flowchart TD
    A["用户上传文档"] --> B["文件存储到 MinIO"]
    B --> C["创建 Document 记录<br/>status=PENDING"]
    C --> D["去重检查"]

    D --> D1{"去重策略?"}
    D1 -->|NONE| E["直接进入解析"]
    D1 -->|BY_NAME| D2["检查文件名是否重复"]
    D1 -->|BY_CONTENT| D3["计算内容哈希<br/>检查是否重复"]
    D1 -->|BY_NAME_OR_CONTENT| D4["同时检查文件名和内容哈希"]

    D2 --> D5{"命中重复?"}
    D3 --> D5
    D4 --> D5
    D5 -->|否| E
    D5 -->|是| D6{"冲突动作?"}
    D6 -->|REJECT| D7["拒绝上传"]
    D6 -->|SKIP| D8["跳过该文件"]
    D6 -->|OVERWRITE| D9["覆盖已有文档"]
    D9 --> E

    E --> F["格式解析"]
    F --> F1{"文件类型"}
    F1 -->|PDF| F2["PDF Parser<br/>(含OCR图片识别)"]
    F1 -->|Word| F3["DOCX/DOC Parser"]
    F1 -->|Excel| F4["XLSX/XLS Parser<br/>(表格转文本)"]
    F1 -->|PPT| F5["PPTX Parser"]
    F1 -->|Markdown| F6["MD Parser"]
    F1 -->|HTML| F7["HTML Parser<br/>(去标签)"]
    F1 -->|TXT/CSV| F8["纯文本 Parser"]

    F2 --> G["文本分片"]
    F3 --> G
    F4 --> G
    F5 --> G
    F6 --> G
    F7 --> G
    F8 --> G

    G --> H["向量嵌入<br/>Embedding Model"]
    H --> I["双索引存储"]
    I --> I1["向量索引<br/>PgVector / Milvus / ES"]
    I --> I2["关键词索引<br/>Elasticsearch / PG 全文"]
    I1 --> J["更新 Document status=SUCCESS"]
    I2 --> J

    style A fill:#3498db,color:#fff
    style J fill:#2ecc71,color:#fff
    style D7 fill:#e74c3c,color:#fff
    style D8 fill:#95a5a6,color:#fff
```

### 支持的文档格式

| 格式 | 扩展名 | 解析器 | 特性 |
|------|--------|--------|------|
| PDF | `.pdf` | PDF Parser | 支持文本提取、图片 OCR |
| Word | `.docx` `.doc` | DOCX/DOC Parser | 支持表格和图片 |
| Excel | `.xlsx` `.xls` | Excel Parser | 表格转结构化文本 |
| PowerPoint | `.pptx` | PPTX Parser | 幻灯片文本提取 |
| Markdown | `.md` | MD Parser | 保留结构标记 |
| HTML | `.html` | HTML Parser | 去除标签，保留文本 |
| 纯文本 | `.txt` | 直接读取 | 最简单的格式 |
| CSV | `.csv` | CSV Parser | 表格转文本 |

### 文档处理状态机

```mermaid
stateDiagram-v2
    [*] --> PENDING: 文档上传
    PENDING --> PARSING: 开始解析
    PARSING --> PROCESSING: 解析完成，开始分片+嵌入
    PROCESSING --> SUCCESS: 索引完成
    PARSING --> FAILED: 解析失败
    PROCESSING --> FAILED: 嵌入/索引失败
    FAILED --> PARSING: 重新解析 (reprocess)
    SUCCESS --> PARSING: 重新解析 (reprocess)

    note right of PENDING
        status = 0
        刚上传，等待处理
    end note
    note right of PARSING
        status = 1
        正在解析文档内容
    end note
    note right of PROCESSING
        status = 2
        分片+向量嵌入中
    end note
    note right of SUCCESS
        status = 3
        处理完成，可检索
    end note
    note right of FAILED
        status = 4
        处理失败，可重试
    end note
```

### 文本分片策略

Snail AI 提供 **4 种分片策略**，满足不同文档类型和检索需求：

```mermaid
graph TB
    subgraph Default["default - 递归字符分片"]
        D1["按 \\n\\n → \\n → . → 空格<br/>层层递归分割"]
        D2["maxChunkTokens = 500<br/>chunkOverlap = 50"]
    end

    subgraph Delimiter["delimiter - 自定义分隔符"]
        E1["按用户指定分隔符<br/>一级切分"]
        E2["customDelimiter = '---'<br/>或 '\\n## '"]
    end

    subgraph Regex["regex - 正则表达式"]
        F1["按 Java 正则<br/>一级切分"]
        F2["chunkRegex = '(?m)^#{1,3} '"]
    end

    subgraph Smart["smart - 语义智能分片"]
        G1["调用对话模型<br/>输出语义边界 JSON"]
        G2["chunkModelId = 模型ID<br/>mergeShortSegments = true"]
    end

    style Default fill:#3498db,color:#fff
    style Delimiter fill:#2ecc71,color:#fff
    style Regex fill:#f39c12,color:#fff
    style Smart fill:#e74c3c,color:#fff
```

| 策略 | 标识 | 原理 | 适用场景 |
|------|------|------|----------|
| **递归字符** | `default` | 按分隔符优先级递归分割（段落→句子→词） | 通用场景，推荐默认 |
| **自定义分隔符** | `delimiter` | 按用户指定的分隔符进行一级切分 | 结构化文档（如 FAQ、条款） |
| **正则表达式** | `regex` | 按正则表达式匹配进行一级切分 | 按章节/标题分割 |
| **语义智能分片** | `smart` | 调用对话模型识别语义边界再切分 | 高质量知识库，对准确性要求高 |

**分片参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `maxChunkTokens` | 单片最大 Token 数 | 500 |
| `chunkOverlap` | 相邻片段重叠 Token 数 | 50 |
| `customDelimiter` | 自定义分隔符（delimiter 模式） | - |
| `chunkRegex` | 正则表达式（regex 模式） | - |
| `chunkModelId` | 语义分片模型 ID（smart 模式） | - |
| `mergeShortSegments` | 是否合并过短的片段 | `true` |
| `imageOcr` | 是否对图片进行 OCR | `false` |

### 去重架构

文档上传采用 **Preview-Commit 两阶段模式**，在上传前检测重复并提供用户二次确认的能力：

```mermaid
sequenceDiagram
    participant User as 用户
    participant Server as Server
    participant Store as MinIO
    participant DB as 数据库

    Note over User,DB: 阶段一：预览 (Preview)
    User->>Server: POST /document/upload/preview<br/>(files, ragId, dedupStrategy, dedupAction)
    Server->>Store: 存储为临时资源
    Server->>DB: 查询已有文档

    alt 按文件名去重 (BY_NAME)
        Server->>DB: SELECT WHERE name = ?
    else 按内容去重 (BY_CONTENT)
        Server->>Server: 计算内容哈希
        Server->>DB: SELECT WHERE contentHash = ?
    else 双重去重 (BY_NAME_OR_CONTENT)
        Server->>DB: SELECT WHERE name = ? OR contentHash = ?
    end

    Server-->>User: PreviewResult<br/>{previewToken, items: [{decision, matchType, conflictDocumentId}]}

    Note over User,DB: 阶段二：确认提交 (Commit)
    User->>User: 查看每个文件的预测结果<br/>决定 NEW / SKIP / OVERWRITE
    User->>Server: POST /document/upload/commit<br/>{previewToken, items: [{tempResourceId, decision}]}
    Server->>Server: TOCTOU 再次检查
    Server->>DB: 按最终 decision 执行
    Server-->>User: CommitResult<br/>{conflictChanged, items}
```

**去重决策矩阵：**

| 去重策略 | 匹配方式 | 说明 |
|----------|----------|------|
| `NONE (0)` | 不检查 | 所有文件直接入库 |
| `BY_NAME (1)` | 文件名完全匹配 | 检查同名文件是否已存在 |
| `BY_CONTENT (2)` | 内容哈希匹配 | 计算文档内容哈希值比对 |
| `BY_NAME_OR_CONTENT (3)` | 文件名或内容任意匹配 | 最严格模式 |

| 冲突动作 | 行为 |
|----------|------|
| `REJECT (0)` | 拒绝上传，返回错误 |
| `SKIP (1)` | 静默跳过重复文件 |
| `OVERWRITE (2)` | 删除旧文档，上传新文档 |

## 检索流水线

### 完整流程

```mermaid
flowchart TD
    A["用户查询 (Query)"] --> B{"是否启用 Query 改写?"}
    B -->|是| C["LLM Query 改写<br/>优化检索关键词"]
    B -->|否| D["使用原始 Query"]
    C --> D

    D --> E["并行检索"]
    E --> E1["向量检索<br/>Embedding → Vector Search<br/>语义相似度匹配"]
    E --> E2["BM25 检索<br/>关键词全文检索<br/>（需启用搜索引擎）"]

    E1 --> F["结果融合"]
    E2 --> F

    F --> F1{"融合策略?"}
    F1 -->|RRF| F2["Reciprocal Rank Fusion<br/>倒数排名融合"]
    F1 -->|WEIGHTED_SUM| F3["加权求和融合<br/>denseWeight 控制权重"]

    F2 --> G{"是否启用 Rerank?"}
    F3 --> G
    G -->|否| H["直接使用融合结果"]
    G -->|是| I["Reranker 模型重排序"]
    I --> H

    H --> J{"是否启用阈值过滤?"}
    J -->|否| K["返回 Top-K 结果"]
    J -->|是| L["过滤低于阈值的结果"]
    L --> K

    K --> M["构建知识上下文"]
    M --> N["注入到 Agent Prompt"]

    style A fill:#3498db,color:#fff
    style N fill:#2ecc71,color:#fff
    style E1 fill:#9b59b6,color:#fff
    style E2 fill:#f39c12,color:#fff
```

### 检索参数

```mermaid
graph LR
    subgraph SearchConfig["检索配置 (searchParams)"]
        direction TB
        RC["resultCount<br/>返回数量<br/>默认: 5"]
        QR["questionRewrite<br/>Query 改写<br/>默认: false"]
        FS["fusionStrategy<br/>融合策略<br/>RRF / WEIGHTED_SUM"]
        DW["denseWeight<br/>向量权重<br/>默认: 0.7"]
        RK["rrfK<br/>RRF 常数 K<br/>默认: 60"]
        RE["rerankEnabled<br/>重排序开关<br/>默认: false"]
        RM["rerankModelId<br/>Reranker 模型"]
        ERC["enterRerankCount<br/>进入重排序的数量<br/>默认: 20"]
        TE["thresholdEnabled<br/>阈值过滤开关<br/>默认: false"]
        TV["threshold<br/>相似度阈值<br/>默认: 0.5"]
    end

    style SearchConfig fill:#ecf0f1,color:#2c3e50
```

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| `resultCount` | int | 最终返回的知识片段数量 | 5 |
| `questionRewrite` | boolean | 是否启用 LLM Query 改写 | false |
| `fusionStrategy` | enum | 融合策略：`RRF` / `WEIGHTED_SUM` | RRF |
| `denseWeight` | float | 向量检索权重（`WEIGHTED_SUM` 模式下） | 0.7 |
| `rrfK` | int | RRF 融合常数 K | 60 |
| `rerankEnabled` | boolean | 是否启用 Reranker 重排序 | false |
| `rerankModelId` | int | Reranker 模型 ID | - |
| `enterRerankCount` | int | 进入 Reranker 的候选数量 | 20 |
| `thresholdEnabled` | boolean | 是否启用相似度阈值过滤 | false |
| `threshold` | float | 相似度阈值，低于此值的结果被过滤 | 0.5 |

### 混合检索详解

#### 向量检索

```mermaid
flowchart LR
    Q["Query 文本"] --> EMB["Embedding Model<br/>向量化"]
    EMB --> VEC["查询向量<br/>[0.12, -0.34, ...]"]
    VEC --> VS["Vector Store<br/>ANN 近似最近邻搜索"]
    VS --> RES["Top-K 结果<br/>(含相似度分数)"]

    style Q fill:#3498db,color:#fff
    style RES fill:#2ecc71,color:#fff
```

向量检索基于语义相似度，能够理解查询意图，召回语义相关但用词不同的知识片段。

#### BM25 检索

```mermaid
flowchart LR
    Q["Query 文本"] --> TOK["分词<br/>(中文分词 / 英文分词)"]
    TOK --> BM["BM25 算法<br/>TF-IDF 变种"]
    BM --> SE["Search Engine<br/>Elasticsearch / PG 全文"]
    SE --> RES["Top-K 结果<br/>(含 BM25 分数)"]

    style Q fill:#f39c12,color:#fff
    style RES fill:#2ecc71,color:#fff
```

BM25 检索基于关键词匹配，对精确术语、名称、编号等特别有效。

#### RRF 融合算法

**RRF（Reciprocal Rank Fusion）** 是一种不依赖分数量纲的排名融合算法：

```
RRF_score(d) = Σ 1 / (k + rank_i(d))
```

其中 `k` 是常数（默认 60），`rank_i(d)` 是文档 d 在第 i 个检索系统中的排名。

```mermaid
graph TB
    subgraph VectorResult["向量检索结果"]
        V1["Doc A (rank 1)"]
        V2["Doc C (rank 2)"]
        V3["Doc E (rank 3)"]
    end

    subgraph BM25Result["BM25 检索结果"]
        B1["Doc B (rank 1)"]
        B2["Doc A (rank 2)"]
        B3["Doc D (rank 3)"]
    end

    subgraph RRF["RRF 融合 (k=60)"]
        R1["Doc A: 1/61 + 1/62 = 0.0326"]
        R2["Doc B: 0 + 1/61 = 0.0164"]
        R3["Doc C: 1/62 + 0 = 0.0161"]
        R4["Doc D: 0 + 1/63 = 0.0159"]
        R5["Doc E: 1/63 + 0 = 0.0159"]
    end

    subgraph FinalRank["最终排名"]
        F1["1. Doc A ✓"]
        F2["2. Doc B"]
        F3["3. Doc C"]
        F4["4. Doc D"]
        F5["5. Doc E"]
    end

    VectorResult --> RRF
    BM25Result --> RRF
    RRF --> FinalRank

    style FinalRank fill:#2ecc71,color:#fff
```

#### 加权求和融合

当选择 `WEIGHTED_SUM` 策略时，两种检索的分数按权重加权求和：

```
final_score(d) = denseWeight × vector_score(d) + (1 - denseWeight) × bm25_score(d)
```

**注意：** 使用加权融合前，需要对两种分数进行归一化处理（Min-Max Normalization），确保量纲一致。

### Reranker 重排序

```mermaid
sequenceDiagram
    participant Pipeline as 检索流水线
    participant Reranker as Reranker 模型

    Pipeline->>Pipeline: 融合后取 Top-enterRerankCount(20)
    Pipeline->>Reranker: 发送 (query, [doc1, doc2, ..., doc20])
    Reranker->>Reranker: Cross-Encoder 逐对打分
    Reranker-->>Pipeline: 返回重排后的排序 + 分数
    Pipeline->>Pipeline: 取 Top-resultCount(5)
    Pipeline->>Pipeline: 阈值过滤 (若启用)
```

Reranker 模型使用 Cross-Encoder 架构，对 query 和每个候选文档联合编码，产生更精确的相关性分数。相比向量检索的 Bi-Encoder（独立编码后计算余弦相似度），Cross-Encoder 的准确度更高但计算成本也更高，因此采用"粗筛 + 精排"的两阶段架构。

### 检索性能指标

每次检索返回详细的性能指标（`SearchMetrics`），用于调优和监控：

```mermaid
graph LR
    subgraph Metrics["检索性能指标"]
        direction TB
        M1["embeddingMs: 向量嵌入耗时"]
        M2["vectorSearchMs: 向量检索耗时"]
        M3["bm25SearchMs: BM25 检索耗时"]
        M4["fusionMs: 融合计算耗时"]
        M5["rerankMs: 重排序耗时"]
        M6["totalMs: 总耗时"]
        M7["vectorHitCount: 向量检索命中数"]
        M8["bm25HitCount: BM25 检索命中数"]
        M9["finalCount: 最终返回数"]
    end

    style Metrics fill:#ecf0f1,color:#2c3e50
```

## 存储后端

### 向量存储

```mermaid
graph TB
    subgraph VectorStores["向量存储后端"]
        PGV["PgVector<br/>PostgreSQL 扩展<br/>适合中小规模"]
        MIL["Milvus<br/>专业向量数据库<br/>适合大规模"]
        ES_V["Elasticsearch<br/>向量检索模式<br/>适合混合检索"]
    end

    subgraph SearchEngines["搜索引擎后端"]
        ES_S["Elasticsearch<br/>BM25 全文检索"]
        PG_FT["PG 全文<br/>PostgreSQL 内置全文检索"]
    end

    RAG["RAG 知识库"] --> PGV
    RAG --> MIL
    RAG --> ES_V
    RAG --> ES_S
    RAG --> PG_FT

    style VectorStores fill:#3498db,color:#fff
    style SearchEngines fill:#f39c12,color:#fff
```

| 存储后端 | 类别 | 适用场景 | 特点 |
|----------|------|----------|------|
| **PgVector** | 向量存储 | 中小规模，与 PG 共用实例 | 部署简单，维护成本低 |
| **Milvus** | 向量存储 | 大规模向量检索（百万级以上） | 高性能，分布式扩展 |
| **Elasticsearch** | 向量+全文 | 需要混合检索的场景 | 同时支持向量和关键词检索 |
| **PG 全文** | 搜索引擎 | 轻量 BM25，不额外部署 ES | 与 PG 共用实例 |

### 存储实例配置

每个 RAG 知识库可独立绑定不同的存储实例：

```mermaid
graph TB
    RAG1["知识库: 产品文档"] -->|vectorStoreInstanceId=1| VS1["PgVector 实例"]
    RAG1 -->|searchEngineInstanceId=3| SE1["ES 全文实例"]

    RAG2["知识库: 技术Wiki"] -->|vectorStoreInstanceId=2| VS2["Milvus 实例"]
    RAG2 -->|searchEngineInstanceId=3| SE1

    RAG3["知识库: FAQ"] -->|vectorStoreInstanceId=1| VS1
    RAG3 -->|searchEngineEnable=false| NONE["不启用搜索引擎"]

    style RAG1 fill:#3498db,color:#fff
    style RAG2 fill:#2ecc71,color:#fff
    style RAG3 fill:#f39c12,color:#fff
```

## 端到端数据流

```mermaid
graph TB
    subgraph Upload["1. 文档上传"]
        U1["PDF / Word / Excel..."]
    end

    subgraph Store["2. 文件存储"]
        U1 --> MINIO["MinIO / 本地文件系统"]
    end

    subgraph Parse["3. 文档解析"]
        MINIO --> PARSER["DocumentParser"]
        PARSER --> TEXT["提取纯文本"]
    end

    subgraph Split["4. 文本分片"]
        TEXT --> SPLITTER["TextSplitter<br/>(4 种策略)"]
        SPLITTER --> CHUNKS["Chunk 1 | Chunk 2 | ... | Chunk N"]
    end

    subgraph Embed["5. 向量嵌入"]
        CHUNKS --> EMB_MODEL["Embedding Model"]
        EMB_MODEL --> VECTORS["向量 1 | 向量 2 | ... | 向量 N"]
    end

    subgraph Index["6. 双重索引"]
        VECTORS --> VDB["向量数据库<br/>PgVector / Milvus / ES"]
        CHUNKS --> FTDB["全文索引<br/>ES / PG 全文"]
    end

    subgraph Retrieve["7. 检索召回"]
        QUERY["用户查询"]
        QUERY --> EMB2["Query 嵌入"]
        EMB2 --> VSEARCH["向量 ANN 搜索"]
        QUERY --> BM25["BM25 搜索"]
        VDB -.-> VSEARCH
        FTDB -.-> BM25
        VSEARCH --> MERGE["融合 + Rerank"]
        BM25 --> MERGE
    end

    subgraph Inject["8. 上下文注入"]
        MERGE --> CTX["知识上下文"]
        CTX --> PROMPT["Agent System Prompt"]
    end

    style Upload fill:#3498db,color:#fff
    style Inject fill:#2ecc71,color:#fff
```
