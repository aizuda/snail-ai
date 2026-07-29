# Snail AI 架构设计指南

本文档详细说明 Snail AI 项目的架构设计、模块划分和依赖关系。

## 目录

- [整体架构](#整体架构)
- [模块职责](#模块职责)
- [分层架构](#分层架构)
- [包结构规范](#包结构规范)
- [模块间通信](#模块间通信)
- [依赖注入](#依赖注入)
- [Agent 与 Server 通信](#agent-与-server-通信)

## 整体架构

Snail AI 采用**模块化分层架构**，将系统划分为 5 个顶层模块：

```
snail-ai (根 POM)
├── snail-ai-commons          # 通用基础层
├── snail-ai-models           # 模型适配层（对 Spring AI 的二次封装）
├── snail-ai-server           # 服务端（管理后台 + 业务逻辑 + 数据库）
├── snail-ai-agent            # Agent 客户端（独立部署的 LLM 执行进程）
└── snail-ai-starter          # 最终启动模块（打包入口）
```

### 架构设计原则

1. **模块化**：每个模块有明确的职责边界，按功能垂直拆分
2. **分层清晰**：上层依赖下层，同层模块避免循环依赖
3. **低耦合高内聚**：模块间通过接口/DTO 通信，不直接访问内部实现
4. **可扩展性**：模型适配器、向量存储、搜索存储均采用 SPI 模式，便于扩展
5. **可维护性**：代码组织清晰，遵循统一的包结构和命名规范

### 技术栈

| 层次 | 技术选型 |
|------|---------|
| 框架 | Spring Boot 4.1 + Spring AI 2.0 |
| ORM | MyBatis-Plus 3.5 |
| 通信 | gRPC（Agent↔Server）+ REST（Server↔前端） |
| 数据库 | MySQL / PostgreSQL（业务数据） |
| 向量库 | Elasticsearch / Milvus / PGVector |
| AI 模型 | OpenAI 兼容接口（通义千问、DeepSeek 等） |
| 前端 | Vue 3 + TypeScript + Ant Design Vue |

---

## 模块职责

### 1. snail-ai-commons — 通用基础层

```
snail-ai-commons
├── snail-ai-commons-core     # DTO、枚举、工具类、通用接口
└── snail-ai-commons-grpc     # gRPC 通信封装（Agent↔Server 通信）
```

**snail-ai-commons-core** 职责：
- 全局枚举（StatusEnum、RoleEnum 等）
- 通用 DTO（ChatStreamResponse、ConversationRecordRequest 等）
- 工具类（JsonUtil、DateUtil 等）
- 异常体系（SnailAiException 等）
- 通用模型（Result、PageResult 等）

**snail-ai-commons-grpc** 职责：
- gRPC 服务端/客户端封装
- Proto 定义和自动生成代码
- GrpcRequestDispatcher 请求分发
- StreamObserver 封装

**依赖关系**：不依赖任何其他内部模块，是最底层模块

---

### 2. snail-ai-models — 模型适配层

对 Spring AI 的二次封装，提供统一的模型访问接口。

```
snail-ai-models
├── snail-ai-model-common                  # 模型公共接口（ChatModelSpec、ModelAdapter）
├── snail-ai-model-chat/                   # Chat 模型
│   ├── snail-ai-model-chat-core           # 核心：自定义 OpenAiChatModel、ChatModelRuntime
│   ├── snail-ai-model-chat-provider-openai-compatible  # OpenAI 兼容适配器
│   └── snail-ai-model-chat-starter        # Spring Boot 自动装配
├── snail-ai-model-embedding/              # Embedding 模型
│   ├── snail-ai-model-embedding-core
│   ├── snail-ai-model-embedding-provider-openai-compatible
│   └── snail-ai-model-embedding-starter
└── snail-ai-model-rerank/                 # Rerank 模型
    ├── snail-ai-model-rerank-core
    ├── snail-ai-model-rerank-provider-qwen
    └── snail-ai-model-rerank-starter
```

**核心设计**：
- `ChatModelSpec`：模型规格描述（baseUrl、apiKey、modelKey 等）
- `ChatModelAdapter`：模型适配器接口，不同供应商实现不同 adapter
- `ChatModelRuntime`：模型运行时，根据 adapterKey 选择合适的适配器创建 ChatModel
- `OpenAiCompatibleChatModelAdapter`：OpenAI 兼容适配器，支持通义千问、DeepSeek 等

**依赖关系**：
- 依赖 `snail-ai-commons-core`
- 依赖 Spring AI（`spring-ai-openai`、`spring-ai-client-chat`）
- 被 `snail-ai-server`（feature-model）和 `snail-ai-agent`（executor-model）共同依赖

---

### 3. snail-ai-server — 服务端

管理后台 + 业务逻辑 + 数据库访问，是系统的核心业务模块。

```
snail-ai-server
├── snail-ai-server-persistence/           # 持久化层（数据库）
│   ├── snail-ai-biz-storage/              # 业务数据库
│   │   ├── snail-ai-biz-template          # MyBatis-Plus 通用模板（PO、Mapper）
│   │   ├── snail-ai-mysql-storage         # MySQL 驱动实现
│   │   └── snail-ai-postgres-storage      # PostgreSQL 驱动实现
│   ├── snail-ai-vector-storage/           # 向量数据库
│   │   ├── snail-ai-vector-template       # 向量存储统一接口
│   │   ├── snail-ai-vector-es-storage     # Elasticsearch 实现
│   │   ├── snail-ai-vector-milvus-storage # Milvus 实现
│   │   └── snail-ai-vector-pg-storage     # PGVector 实现
│   └── snail-ai-search-storage/           # 全文搜索
│       ├── snail-ai-search-template       # 搜索统一接口
│       └── snail-ai-search-elasticsearch-storage  # ES 实现
├── snail-ai-server-features/              # 业务功能层
│   ├── snail-ai-feature-common            # 功能公共（日志、配置）
│   ├── snail-ai-feature-model             # 模型管理（CRUD、调用封装）
│   ├── snail-ai-feature-agent             # Agent 对话（核心业务：gRPC 调度、结果持久化）
│   ├── snail-ai-feature-rag               # RAG 检索增强（文档解析、向量检索）
│   ├── snail-ai-feature-skill             # 技能管理（MCP Server/Tool）
│   ├── snail-ai-feature-memory            # 记忆管理（短期/长期记忆）
│   └── snail-ai-feature-resource          # 资源管理（文件上传 MinIO）
├── snail-ai-server-admin                  # 管理后台 REST API（Controller）
└── snail-ai-server-openapi                # 开放 API（对外接口、JWT 鉴权）
```

#### 持久化层（snail-ai-server-persistence）

采用**模板模式**，通过 SPI 机制支持多种数据库：

| 存储类型 | 模板接口 | MySQL 实现 | PostgreSQL 实现 | ES 实现 | Milvus 实现 |
|---------|---------|-----------|----------------|--------|------------|
| 业务数据 | biz-template | mysql-storage | postgres-storage | — | — |
| 向量数据 | vector-template | — | vector-pg-storage | vector-es-storage | vector-milvus-storage |
| 全文搜索 | search-template | — | — | search-elasticsearch-storage | — |

#### 业务功能层（snail-ai-server-features）

| 模块 | 职责 | 关键类 |
|------|------|--------|
| feature-common | 日志、配置、工具 | AbstractLog、LogStrategy |
| feature-model | 模型 CRUD + 调用封装 | AiModelConfigService、ChatClientBuilder |
| feature-agent | Agent 对话核心业务 | AgentChatService、ChatStreamObserver、ChatResultPersistService |
| feature-rag | RAG 检索增强 | RagService、DocumentParser、VectorStore |
| feature-skill | 技能管理 | SkillService、McpServerService |
| feature-memory | 记忆管理 | ShortTermMemoryStore、LongTermMemoryStore |
| feature-resource | 文件资源管理 | ResourceService（MinIO） |

#### 管理后台（snail-ai-server-admin）

提供 RESTful API，是前端的入口：

- Controller 层：AgentController、ModelController、MemoryController 等
- Service 层：AgentService、ModelService 等（调用 feature 层）
- VO 层：请求/响应视图对象

#### 开放 API（snail-ai-server-openapi）

对外提供的第三方接口，基于 JWT 鉴权：
- OpenApiChatService：对话 API
- OpenApiConversationService：会话管理 API

**依赖关系**：
- server-admin 依赖所有 feature-* 模块
- server-openapi 依赖 server-admin
- feature-* 依赖 biz-template 和对应 models 模块
- biz-template 依赖 commons-core 和 mybatis-plus

---

### 4. snail-ai-agent — Agent 客户端

独立部署的 LLM 执行进程，通过 gRPC 与 Server 通信。

```
snail-ai-agent
├── snail-ai-agent-common                  # Agent 公共（gRPC 客户端、心跳、上下文）
├── snail-ai-agent-executor/               # 执行引擎
│   ├── snail-ai-agent-executor-model      # 模型工厂（调用 Models 层创建 ChatModel）
│   ├── snail-ai-agent-executor-core       # 核心：Advisor 链、流式执行、gRPC Handler
│   └── snail-ai-agent-executor-starter    # 自动装配
├── snail-ai-agent-openapi/                # OpenAPI 集成（Agent 端）
│   ├── snail-ai-openapi-core
│   └── snail-ai-openapi-starter
└── snail-ai-agent-chat/                   # 聊天 API
    ├── snail-ai-agent-chat-api
    └── snail-ai-agent-chat-starter
```

**核心设计**：

Agent 端采用 **Advisor 责任链** 模式处理流式请求：

```
ChatClient 请求
    ↓
InterceptorChainAdvisor    → 拦截器链（日志等）
    ↓
TokenUsageCollectorAdvisor → 提取 token 使用量（input/output/cache）
    ↓
ThinkingCollectorAdvisor   → 收集思维链/推理过程
    ↓
StreamChunkForwarderAdvisor → 转发文本 chunk 到 gRPC 流
    ↓
LLM 模型调用
```

**关键类**：
- `ClientChatExecutor`：流式执行引擎，组装 ChatClient 和 Advisor 链
- `ClientStreamExecutionContext`：单次流式调用的累积状态（文本、token、工具调用）
- `ChatDispatchStreamingHandler`：gRPC 流式请求处理，桥接 Agent 和 Server
- `ChatSessionRuntime`：会话运行时，管理工具准备和资源清理

**依赖关系**：
- agent-common 依赖 commons-core 和 commons-grpc
- agent-executor-model 依赖 model-chat-core（Models 层）
- agent-executor-core 依赖 agent-common 和 agent-executor-model
- agent-executor-starter 聚合所有 executor 模块并提供自动装配

---

### 5. snail-ai-starter — 启动入口

最终打包成 Spring Boot JAR 的入口模块，不包含业务代码。

```
snail-ai-starter
└── 依赖：server-admin + server-openapi + mysql/postgres-storage + actuator + mcp-server
```

---

## 分层架构

### 整体分层

```
┌─────────────────────────────────────────────────────────────────┐
│                        snail-ai-starter                         │  ← 启动入口
└───────────────────────────────┬─────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ server-admin  │    │  server-openapi  │    │  agent-executor  │  ← API/执行层
│  (REST API)   │    │  (开放 API)      │    │  (LLM 执行)     │
└───────┬───────┘    └────────┬─────────┘    └────────┬─────────┘
        │                     │                       │
        ▼                     ▼                       ▼
┌─────────────────────────────────────┐    ┌──────────────────┐
│       server-features               │    │  agent-common    │  ← 业务功能层
│ agent│model│rag│skill│memory│resource│    │  (gRPC 客户端)   │
└───────────────┬─────────────────────┘    └────────┬─────────┘
                │                                   │
                ▼                                   ▼
┌─────────────────────────────────────┐    ┌──────────────────┐
│     server-persistence              │    │  models (chat/   │  ← 模型/数据层
│  biz│vector│search (template+SPI)   │    │  embedding/rerank)│
└───────────────┬─────────────────────┘    └────────┬─────────┘
                │                                   │
                ▼                                   ▼
┌─────────────────────────────────────┐    ┌──────────────────┐
│         commons-core                │◄───│  Spring AI       │  ← 基础层
│    DTO│枚举│工具类│通用接口          │    │  (框架)          │
└─────────────────────────────────────┘    └──────────────────┘
        ▲
        │
┌─────────────────────────────────────┐
│         commons-grpc                │  ← 通信层
│    gRPC 封装│Proto│分发器            │
└─────────────────────────────────────┘
```

### 数据流向

**用户请求流程（前端 → Server → Agent → LLM）**：

```
前端 HTTP 请求
    ↓
server-admin Controller（参数验证）
    ↓
server-admin Service（业务逻辑）
    ↓
feature-agent AgentChatService（构建 gRPC 请求）
    ↓ gRPC
agent ChatDispatchStreamingHandler（接收请求）
    ↓
agent ClientChatExecutor（Advisor 链 + LLM 调用）
    ↓ 流式响应
agent ChatDispatchStreamingHandler（发送 completion）
    ↓ gRPC
server ChatStreamObserver（接收响应）
    ↓
ChatResultPersistService（持久化对话记录）
    ↓ SSE
前端流式展示
```

### VO/DTO/PO 转换

```
前端 Request
    ↓
Controller 层：RequestVO / QueryVO
    ↓
Service 层：DTO / Command
    ↓
Mapper 层：PO（对应数据库表）
    ↓
Database
    ↓
PO → DTO → ResponseVO → 前端
```

---

## 包结构规范

### Server 端包结构

```
com.aizuda.snail.ai
├── admin/                          # server-admin 模块
│   ├── controller/                 # REST 控制器
│   ├── service/                    # 业务服务
│   ├── vo/                         # 视图对象（按功能分子包）
│   └── handler/                    # 异常处理器
├── feature/                        # server-features 模块
│   ├── agent/                      # Agent 对话
│   │   ├── chain/                  # 处理链（ChatStreamObserver 等）
│   │   ├── callback/               # 回调处理
│   │   └── persist/                # 持久化（ChatResultPersistService）
│   ├── model/                      # 模型管理
│   ├── rag/                        # RAG
│   ├── skill/                      # 技能
│   ├── memory/                     # 记忆
│   └── resource/                   # 资源
├── persistence/                    # server-persistence 模块
│   ├── admin/po/                   # 管理相关 PO
│   ├── agent/po/                   # Agent 相关 PO
│   ├── agent/mapper/               # Agent 相关 Mapper
│   ├── model/po/                   # 模型相关 PO
│   └── model/mapper/               # 模型相关 Mapper
└── openapi/                        # server-openapi 模块
    ├── controller/                 # 开放 API 控制器
    └── service/                    # 开放 API 服务
```

### Agent 端包结构

```
com.aizuda.snail.ai.agent
├── common/                         # agent-common 模块
│   ├── rpc/                        # gRPC 客户端
│   ├── context/                    # 上下文（AgentChatContextHolder）
│   ├── counter/                    # 计数器（ActiveChatCounter）
│   └── config/                     # 配置（SnailAiAgentProperties）
├── core/                           # agent-executor-core 模块
│   ├── advisor/                    # Advisor 链
│   │   ├── TokenUsageCollectorAdvisor
│   │   ├── ThinkingCollectorAdvisor
│   │   ├── StreamChunkForwarderAdvisor
│   │   └── InterceptorChainAdvisor
│   ├── executor/                   # 执行引擎
│   │   ├── ClientChatExecutor
│   │   ├── client/                 # ChatClient 工厂
│   │   ├── model/                  # 模型工厂
│   │   ├── prompt/                 # Prompt 工厂
│   │   └── tool/                   # 工具管理
│   ├── grpc/handler/               # gRPC 请求处理
│   ├── runtime/                    # 运行时（ChatSessionRuntime）
│   ├── resolver/                   # 工具解析器
│   └── interceptor/                # 拦截器
└── starter/                        # agent-executor-starter 模块
    └── SnailAiAgentAutoConfiguration  # 自动装配
```

### 命名规范

| 包名 | 用途 | 说明 |
|------|------|------|
| `controller` | REST 控制器 | 仅 server-admin |
| `service` | 业务服务 | 按功能模块分包 |
| `vo` | 视图对象 | 仅 server-admin，按功能分子包 |
| `dto` | 数据传输对象 | commons-core 中定义 |
| `po` | 持久化对象 | 仅 persistence，对应数据库表 |
| `mapper` | MyBatis Mapper | 仅 persistence |
| `enums` | 枚举类型 | 按模块分布 |
| `config` | 配置类 | @Configuration |
| `handler` | 处理器 | 回调、异常处理 |
| `advisor` | Advisor | 仅 agent 端 |
| `resolver` | 解析器 | 工具解析等 |

---

## 模块间通信

### 依赖方向规则

```
[上层模块] 可以依赖 [下层模块]
[下层模块] 不能依赖 [上层模块]
[同层模块] 避免循环依赖
```

### 正确的依赖示例

✅ **允许**：
```
server-admin → feature-agent → biz-template → commons-core
agent-executor-core → agent-common → commons-grpc → commons-core
feature-model → model-chat-starter → model-chat-core → spring-ai-openai
```

❌ **禁止**：
```
commons-core → server-admin    (下层依赖上层)
feature-agent → feature-model  (同层反向依赖，应通过接口)
```

### 核心依赖关系图

```
                    snail-ai-starter (打包入口)
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    server-admin    server-openapi   mysql/postgres-storage
          │              │
          ▼              ▼
    feature-agent ──► feature-model ──► model-chat-starter ──► model-chat-core
          │                                                    (自定义 OpenAiChatModel)
          ▼
    biz-template (PO/Mapper)
          │
          ▼
    commons-core + commons-grpc ←──────── agent-executor-core (Agent 客户端)
                                            │
                                            ▼
                                      agent-executor-model ──► model-chat-core
```

---

## Agent 与 Server 通信

### 通信协议

Agent 和 Server 通过 **gRPC** 通信，支持流式传输。

### 通信流程

```
Server 端                              Agent 端
    │                                      │
    │──── gRPC ChatDispatch ──────────────→│
    │     (ChatDispatchRequest)            │
    │                                      │→ LLM 调用
    │                                      │
    │←──── gRPC Stream (text chunks) ─────│
    │←──── gRPC Stream (thinking) ────────│
    │←──── gRPC Stream (completion) ──────│
    │     (ChatStreamResponse)            │
    │                                      │
```

### 关键 DTO

**ChatDispatchRequest**（Server → Agent）：
- agentConfig：Agent 配置
- modelConfig：模型配置
- conversationId：会话 ID
- userInfo：用户信息
- content：用户输入

**ChatStreamResponse**（Agent → Server）：
- type：text / thinking / completion / error
- content：文本内容
- fullText / fullThinking：完整文本
- promptTokens / completionTokens / cacheTokens：Token 统计
- durationMs：执行时长

---

## 依赖注入

### 使用 Lombok @RequiredArgsConstructor

**推荐方式**（构造函数注入）：

```java
@Service
@RequiredArgsConstructor  // Lombok 自动生成构造函数
public class ChatResultPersistService {

    private final AgentConversationRecordMapper recordMapper;
    private final AgentUsageStatMapper usageStatMapper;
    private final ShortTermMemoryStore shortTermMemoryStore;

    // 无需写构造函数，Lombok 自动生成
}
```

### 为什么使用构造函数注入

✅ **优点**：
1. **不可变性**：使用 `final` 字段，保证线程安全
2. **强制依赖**：构造时必须提供依赖，避免 NPE
3. **易于测试**：可以直接 new 对象进行测试
4. **循环依赖检测**：编译时就能发现循环依赖

❌ **避免使用字段注入**：
```java
// 不推荐
@Autowired
private UserService userService;  // 字段不是 final，可能为 null
```

---

## 存储 SPI 扩展机制

### 向量存储扩展示例

添加新的向量存储（如 Qdrant）只需：

1. 创建 `snail-ai-vector-qdrant-storage` 模块
2. 实现 `vector-template` 中的接口
3. 在 `starter` 中添加依赖

```
snail-ai-vector-storage
├── snail-ai-vector-template       # 统一接口
├── snail-ai-vector-es-storage     # ES 实现
├── snail-ai-vector-pg-storage     # PGVector 实现
├── snail-ai-vector-milvus-storage # Milvus 实现
└── snail-ai-vector-qdrant-storage # 新增：Qdrant 实现
```

---

## 总结

Snail AI 的架构设计遵循以下核心原则：

1. **5 大顶层模块**：commons（基础）、models（模型）、server（服务端）、agent（客户端）、starter（启动）
2. **分层架构**：Controller → Service → Feature → Persistence → Database
3. **依赖单向**：上层依赖下层，避免循环
4. **模板 + SPI**：数据库、向量、搜索均采用模板模式 + SPI 扩展
5. **Advisor 责任链**：Agent 端采用 Advisor 模式处理流式请求
6. **gRPC 通信**：Agent 和 Server 通过 gRPC 流式通信
7. **构造函数注入**：使用 @RequiredArgsConstructor + final 字段

---

**相关文档**：
- `coding-standards.md` — 编码规范
- `database-guide.md` — 数据库设计
- `api-design.md` — API 设计规范
- `模块依赖关系.md` — 模块依赖详细表格
