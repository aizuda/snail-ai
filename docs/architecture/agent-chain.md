# Agent 责任链

## 概述

Snail AI 的 Agent 执行引擎基于**责任链模式（Chain of Responsibility）** 设计。每次用户对话请求在 Server 端依次经过 10 个处理器（Handler），每个 Handler 负责一个独立的关注点，处理完毕后将上下文传递给下一个 Handler，最终由 `LlmCallHandler` 将请求分发到 Client 节点执行。

```mermaid
graph LR
    A["InitContext<br/>order=0"] --> B["Conversation<br/>order=5"]
    B --> C["ModelResolve<br/>order=10"]
    C --> D["SystemPrompt<br/>order=15"]
    D --> E["Mcp<br/>order=20"]
    E --> F["SkillAgent<br/>order=30"]
    F --> G["WebSearch<br/>order=35"]
    G --> H["ContextCollector<br/>order=40"]
    H --> I["Rag<br/>order=50"]
    I --> J["LlmCall<br/>order=80"]

    style A fill:#1abc9c,color:#fff
    style B fill:#2ecc71,color:#fff
    style C fill:#3498db,color:#fff
    style D fill:#9b59b6,color:#fff
    style E fill:#e74c3c,color:#fff
    style F fill:#f39c12,color:#fff
    style G fill:#e67e22,color:#fff
    style H fill:#16a085,color:#fff
    style I fill:#8e44ad,color:#fff
    style J fill:#c0392b,color:#fff
```

## 完整调用时序

```mermaid
sequenceDiagram
    participant User as 用户浏览器
    participant Controller as REST Controller
    participant Chain as 责任链引擎
    participant Init as InitContextHandler
    participant Conv as ConversationHandler
    participant Model as ModelResolveHandler
    participant Sys as SystemPromptHandler
    participant Mcp as McpHandler
    participant Skill as SkillAgentChatHandler
    participant Web as WebSearchHandler
    participant Ctx as ContextCollectorHandler
    participant Rag as RagHandler
    participant LLM as LlmCallHandler
    participant Observer as ChatStreamObserver
    participant Client as Client Node

    User->>Controller: POST /agent/{id}/chat<br/>{conversationId, content}
    Controller->>Chain: execute(chatContext)

    Chain->>Init: handle(context)
    Note over Init: 加载 Agent 配置<br/>校验参数<br/>初始化上下文对象

    Chain->>Conv: handle(context)
    Note over Conv: 加载/创建会话<br/>设置会话 ID<br/>管理会话状态

    Chain->>Model: handle(context)
    Note over Model: 解析绑定模型<br/>构建 ModelParameters<br/>验证模型可用性

    Chain->>Sys: handle(context)
    Note over Sys: 组装系统提示词<br/>变量模板替换<br/>注入角色指令

    Chain->>Mcp: handle(context)
    Note over Mcp: 发现 MCP Server<br/>解析工具定义<br/>注入 ToolDefinition 列表

    Chain->>Skill: handle(context)
    Note over Skill: 加载 Skill 文件<br/>解析 SKILL.md 指令<br/>注入技能描述

    Chain->>Web: handle(context)
    Note over Web: 检查联网搜索开关<br/>准备搜索配置

    Chain->>Ctx: handle(context)
    Note over Ctx: 收集短期记忆(滑动窗口)<br/>检索长期记忆<br/>汇总所有上下文

    Chain->>Rag: handle(context)
    Note over Rag: 执行 RAG 检索<br/>注入知识片段<br/>到 context messages

    Chain->>LLM: handle(context)
    LLM->>Observer: 创建 ChatStreamObserver
    LLM->>Client: gRPC StreamChat(request)

    loop 流式输出
        Client-->>Observer: StreamResponse(chunk)
        Observer-->>User: SSE {type:"text", content:"..."}
    end

    Client-->>Observer: StreamComplete
    Observer->>Observer: 保存对话记录
    Observer-->>User: SSE stream end
```

## Handler 详解

### 1. InitContextHandler (order=0)

**职责：** 初始化整个对话请求的上下文对象。

```mermaid
flowchart TD
    A["接收 ChatRequest"] --> B["查询 Agent 配置"]
    B --> C["校验 Agent 状态"]
    C --> D{"Agent 是否可用?"}
    D -->|否| E["抛出异常：Agent 不可用"]
    D -->|是| F["创建 ChatContext"]
    F --> G["设置 agentId, userId"]
    G --> H["加载 AgentConfig"]
    H --> I["传递给下一个 Handler"]
```

**ChatContext 核心字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `agentId` | Long | 智能体 ID |
| `userId` | Long | 当前用户 ID |
| `conversationId` | String | 会话 ID |
| `agentConfig` | AgentConfig | 智能体完整配置 |
| `messages` | List\<Message\> | 消息列表（逐步填充） |
| `tools` | List\<ToolDefinition\> | 工具列表（MCP/Skill 注入） |
| `modelParameters` | ModelParameters | 模型调用参数 |
| `systemPrompt` | String | 系统提示词 |
| `metadata` | Map | 扩展元数据 |

### 2. ConversationHandler (order=5)

**职责：** 管理会话状态，加载或创建会话。

```mermaid
flowchart TD
    A["获取 conversationId"] --> B{"conversationId 是否为空?"}
    B -->|是| C["创建新会话<br/>生成 UUID"]
    B -->|否| D["加载已有会话"]
    C --> E["持久化会话记录"]
    D --> F{"会话是否存在?"}
    F -->|否| C
    F -->|是| G["设置会话上下文"]
    E --> G
    G --> H["传递给下一个 Handler"]
```

### 3. ModelResolveHandler (order=10)

**职责：** 解析智能体绑定的模型配置，构建模型调用参数。

```mermaid
flowchart TD
    A["读取 Agent.chatModelId"] --> B["查询 ModelConfig"]
    B --> C{"模型是否存在且启用?"}
    C -->|否| D["使用默认模型"]
    C -->|是| E["解析模型参数"]
    D --> E
    E --> F["构建 ModelParameters"]
    F --> G["设置 apiKey, endpoint"]
    G --> H["设置 temperature, maxTokens 等"]
    H --> I["写入 ChatContext.modelParameters"]
    I --> J["传递给下一个 Handler"]
```

**ModelParameters 结构：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `modelName` | 模型名称 | Agent 绑定的模型 |
| `apiKey` | API 密钥 | 加密存储，运行时解密 |
| `apiEndpoint` | API 地址 | 模型配置中的 endpoint |
| `temperature` | 温度系数 | 0.7 |
| `maxTokens` | 最大输出 Token | 4096 |

### 4. SystemPromptHandler (order=15)

**职责：** 组装系统提示词。

```mermaid
flowchart TD
    A["加载 Agent.instruction"] --> B{"instruction 是否为空?"}
    B -->|是| C["使用默认系统提示词"]
    B -->|否| D["模板变量替换"]
    C --> D
    D --> E["注入日期/时间变量"]
    E --> F["注入用户信息变量"]
    F --> G["生成最终 systemPrompt"]
    G --> H["写入 ChatContext.systemPrompt"]
    H --> I["传递给下一个 Handler"]
```

### 5. McpHandler (order=20)

**职责：** 发现并注册智能体绑定的 MCP 工具。

```mermaid
flowchart TD
    A{"Agent 是否启用 MCP?"}
    A -->|否| SKIP["跳过，传递给下一个 Handler"]
    A -->|是| B["查询绑定的 McpServer 列表"]
    B --> C["遍历每个 McpServer"]
    C --> D["解析工具定义<br/>(name, description, parameters)"]
    D --> E["构建 ToolDefinition"]
    E --> F["添加到 ChatContext.tools"]
    F --> G{"还有更多 McpServer?"}
    G -->|是| C
    G -->|否| H["传递给下一个 Handler"]
```

### 6. SkillAgentChatHandler (order=30)

**职责：** 加载技能指令，将技能内容注入到上下文中。

```mermaid
flowchart TD
    A{"Agent 是否启用 Skill?"}
    A -->|否| SKIP["跳过"]
    A -->|是| B["查询绑定的 Skill 列表"]
    B --> C["遍历每个 Skill"]
    C --> D["读取 SKILL.md 文件"]
    D --> E["解析技能指令"]
    E --> F["注入到 systemPrompt 尾部"]
    F --> G{"还有更多 Skill?"}
    G -->|是| C
    G -->|否| H["传递给下一个 Handler"]
```

### 7. WebSearchHandler (order=35)

**职责：** 准备 Web 搜索能力。

```mermaid
flowchart TD
    A{"Agent 是否启用 WebSearch?"}
    A -->|否| SKIP["跳过"]
    A -->|是| B["注册 WebSearch 工具定义"]
    B --> C["添加到 ChatContext.tools"]
    C --> D["传递给下一个 Handler"]
```

### 8. ContextCollectorHandler (order=40)

**职责：** 收集历史对话和长期记忆，构建完整的消息上下文。

```mermaid
flowchart TD
    A["加载短期记忆<br/>(最近 N 轮对话)"] --> B["shortTermMemorySize<br/>默认 20 条"]
    B --> C{"Agent 是否启用长期记忆?"}
    C -->|否| E["仅使用短期记忆"]
    C -->|是| D["执行长期记忆向量检索<br/>MemoryInjectionAdvisor"]
    D --> E
    E --> F["组装 messages 列表"]
    F --> G["System Prompt<br/>+ 记忆注入<br/>+ 历史对话<br/>+ 当前用户消息"]
    G --> H["传递给下一个 Handler"]
```

### 9. RagHandler (order=50)

**职责：** 执行 RAG 知识检索，将相关知识片段注入到上下文中。

```mermaid
flowchart TD
    A{"Agent 是否启用 RAG?"}
    A -->|否| SKIP["跳过"]
    A -->|是| B["获取绑定的知识库 ragId"]
    B --> C["执行检索 Pipeline"]
    C --> D["query 改写 (可选)"]
    D --> E["并行：向量检索 + BM25"]
    E --> F["融合 + 重排序"]
    F --> G["阈值过滤"]
    G --> H{"检索结果是否为空?"}
    H -->|是| SKIP2["跳过注入"]
    H -->|否| I["构建知识上下文"]
    I --> J["注入到 messages 中<br/>作为 System 消息"]
    J --> K["传递给下一个 Handler"]
    SKIP2 --> K
```

### 10. LlmCallHandler (order=80)

**职责：** 将准备好的上下文通过 gRPC 分发到 Client 节点执行。这是责任链的终端节点。

```mermaid
flowchart TD
    A["从 ChatContext 构建 gRPC ChatRequest"] --> B["选择目标 Client 节点"]
    B --> C["创建 ChatStreamObserver"]
    C --> D["发起 gRPC StreamChat 调用"]
    D --> E["ChatStreamObserver 桥接<br/>gRPC Stream → HTTP SSE"]
    E --> F["等待流式输出完成"]
    F --> G["保存对话记录到数据库"]
    G --> H["完成流式响应"]
```

## ChatStreamObserver：gRPC 到 SSE 的桥接

`ChatStreamObserver` 是连接 gRPC 双向流与 HTTP SSE 的核心组件，负责将 Client 端的流式模型输出实时转发给前端。

```mermaid
sequenceDiagram
    participant Client as Client Node
    participant Observer as ChatStreamObserver
    participant SSE as SSE Emitter
    participant Browser as 浏览器

    Note over Observer: 初始化时持有 SseEmitter 引用

    Client->>Observer: onNext(ChatResponse{type:"thinking", content:"..."})
    Observer->>Observer: 解析消息类型
    Observer->>SSE: send({type:"thinking", content:"..."})
    SSE-->>Browser: data: {"type":"thinking","content":"..."}

    Client->>Observer: onNext(ChatResponse{type:"text", content:"Hello"})
    Observer->>Observer: 累积 answer 内容
    Observer->>SSE: send({type:"text", content:"Hello"})
    SSE-->>Browser: data: {"type":"text","content":"Hello"}

    Client->>Observer: onNext(ChatResponse{type:"text", content:" World"})
    Observer->>Observer: 累积 answer += " World"
    Observer->>SSE: send({type:"text", content:" World"})
    SSE-->>Browser: data: {"type":"text","content":" World"}

    Client->>Observer: onCompleted()
    Observer->>Observer: 保存完整对话记录
    Observer->>Observer: 异步提取长期记忆
    Observer->>SSE: complete()
    SSE-->>Browser: SSE 流结束
```

**Observer 消息类型：**

| type | 说明 | 前端处理 |
|------|------|----------|
| `thinking` | 模型思考过程（推理模型） | 显示在折叠的"思考过程"区域 |
| `text` | 正文内容 | 实时追加到回答区域 |
| `tool_call` | 工具调用信息 | 显示工具调用状态 |
| `error` | 错误信息 | 显示错误提示 |
| `done` | 流结束标记 | 结束加载状态 |

## Client 端 Advisor 流水线

当请求通过 gRPC 到达 Client 节点后，Client 端的 **Advisor 流水线**（基于 Spring AI Advisor 机制）会对请求进行二次处理。当前开源版本内置 5 个主要 Advisor：

```mermaid
graph TB
    subgraph Client["Client 端处理流程"]
        direction TB
        RECV["接收 gRPC ChatRequest"]
        
        subgraph Advisors["Advisor 流水线 (before)"]
            direction TB
            L1["Level 1: MemoryInjectionAdvisor<br/>记忆注入"]
            L2["Level 2: InterceptorChainAdvisor<br/>拦截器链"]
            L3["Level 3: TokenUsageCollectorAdvisor<br/>Token 统计"]
            L4["Level 4: ThinkingCollectorAdvisor<br/>思维内容采集"]
            L5["Level 5: StreamChunkForwarderAdvisor<br/>流式转发"]
        end

        CALL["调用大模型 API"]

        subgraph AdvisorsAfter["Advisor 流水线 (after)"]
            direction TB
            L5A["Level 5: StreamChunkForwarderAdvisor"]
            L4A["Level 4: ThinkingCollectorAdvisor"]
            L3A["Level 3: TokenUsageCollectorAdvisor"]
            L2A["Level 2: InterceptorChainAdvisor"]
            L1A["Level 1: MemoryInjectionAdvisor"]
        end

        SEND["gRPC StreamResponse 回传"]
    end

    RECV --> L1
    L1 --> L2
    L2 --> L3
    L3 --> L4
    L4 --> L5
    L5 --> CALL
    CALL --> L5A
    L5A --> L4A
    L4A --> L3A
    L3A --> L2A
    L2A --> L1A
    L1A --> SEND

    style RECV fill:#3498db,color:#fff
    style CALL fill:#e74c3c,color:#fff
    style SEND fill:#2ecc71,color:#fff
```

### Advisor 层级说明

| 层级 | Advisor | 阶段 | 职责 |
|------|---------|------|------|
| Level 1 | **MemoryInjectionAdvisor** | Before | 注入对话历史与记忆上下文 |
| Level 2 | **InterceptorChainAdvisor** | Before/After | 桥接 `SnailAiInterceptor` 拦截器链 |
| Level 3 | **TokenUsageCollectorAdvisor** | Stream | 统计输入/输出 Token 用量 |
| Level 4 | **ThinkingCollectorAdvisor** | Stream | 收集模型思考内容 |
| Level 5 | **StreamChunkForwarderAdvisor** | Stream | 转发流式响应并累积完整文本 |

### Agentic Loop（工具调用循环）

当大模型返回工具调用请求时，Client 端会进入 **Agentic Loop**：

```mermaid
flowchart TD
    A["发送消息到大模型"] --> B["接收模型响应"]
    B --> C{"是否包含 tool_call?"}
    C -->|否| D["返回最终回答"]
    C -->|是| E["解析 tool_call"]
    E --> F["执行 MCP 工具"]
    F --> G["获取工具返回结果"]
    G --> H["将工具结果追加到消息列表"]
    H --> I["再次发送到大模型"]
    I --> B

    style D fill:#2ecc71,color:#fff
    style F fill:#e74c3c,color:#fff
```

```mermaid
sequenceDiagram
    participant Client as Client Node
    participant LLM as 大模型
    participant MCP as MCP Server

    Client->>LLM: messages + tools
    LLM-->>Client: tool_call: search_web("天气")
    Client->>MCP: 执行 search_web("天气")
    MCP-->>Client: {"result": "今天晴，25°C"}
    Client->>Client: messages.append(tool_result)
    Client->>LLM: messages + tool_result
    LLM-->>Client: "今天天气晴朗，温度25°C..."
    Client-->>Client: 返回最终回答
```

## 扩展点

### 自定义 Handler

开发者可以通过实现 `ChatHandler` 接口并设置 `order` 值来插入自定义 Handler：

```java
@Component
public class CustomHandler implements ChatHandler {
    
    @Override
    public int getOrder() {
        return 25; // 在 McpHandler(20) 之后，SkillHandler(30) 之前
    }
    
    @Override
    public void handle(ChatContext context) {
        // 自定义逻辑
        // 例如：企业合规检查、请求限流、审计日志等
    }
}
```

**可用的 order 空位：**

```mermaid
graph LR
    O0["0<br/>Init"] -.-> O5["5<br/>Conv"]
    O5 -.-> O10["10<br/>Model"]
    O10 -.-> O15["15<br/>Sys"]
    O15 -.-> O20["20<br/>Mcp"]
    O20 -.->|"空位: 21-29"| O30["30<br/>Skill"]
    O30 -.->|"空位: 31-34"| O35["35<br/>Web"]
    O35 -.->|"空位: 36-39"| O40["40<br/>Ctx"]
    O40 -.->|"空位: 41-49"| O50["50<br/>Rag"]
    O50 -.->|"空位: 51-79"| O80["80<br/>LlmCall"]

    style O20 fill:#e74c3c,color:#fff
    style O80 fill:#c0392b,color:#fff
```

### 自定义 Advisor

Client 端的 Advisor 流水线同样支持扩展：

```java
@Component
public class AuditAdvisor implements ChatClientAdvisor {
    
    @Override
    public int getOrder() {
        return 45; // Level 4-5 之间
    }
    
    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request) {
        // 请求前处理：审计日志记录
        auditLog.record(request);
        return request;
    }
    
    @Override
    public AdvisedResponse adviseResponse(AdvisedResponse response) {
        // 响应后处理：合规检查
        complianceCheck(response);
        return response;
    }
}
```

## 责任链 vs Advisor 流水线

两者共同构成 Snail AI 的完整处理流水线，但运行在不同节点、关注不同层面：

```mermaid
graph TB
    subgraph ServerSide["Server 端：责任链 (Handler Chain)"]
        direction LR
        H1["Init"] --> H2["Conv"] --> H3["Model"] --> H4["Sys"]
        H4 --> H5["Mcp"] --> H6["Skill"] --> H7["Web"]
        H7 --> H8["Ctx"] --> H9["Rag"] --> H10["LlmCall"]
    end

    subgraph Network["gRPC 双向流"]
        GRPC["网络传输"]
    end

    subgraph ClientSide["Client 端：Advisor 流水线"]
        direction LR
        A1["Memory"] --> A2["Interceptor"] --> A3["TokenUsage"]
        A3 --> A4["Thinking"] --> A5["StreamForward"]
        A5 --> CALL["模型调用"]
    end

    H10 --> GRPC
    GRPC --> A1

    style ServerSide fill:#4a90d9,color:#fff
    style ClientSide fill:#2ecc71,color:#fff
    style Network fill:#f39c12,color:#fff
```

| 维度 | 责任链 (Server) | Advisor 流水线 (Client) |
|------|-----------------|------------------------|
| **运行节点** | Server | Client |
| **处理对象** | 业务上下文 (ChatContext) | 模型请求/响应 (Request/Response) |
| **关注层面** | 业务编排（知识检索、记忆注入、上下文组装） | 执行控制（日志、Token 统计、思维内容采集） |
| **扩展方式** | 实现 ChatHandler 接口 | 实现 ChatClientAdvisor 接口 |
| **配置方式** | Spring Bean 自动发现 | Spring Bean 自动发现 |
