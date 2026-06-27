# 分布式架构

## 架构概述

Snail AI 采用 **Server-Agent Client** 分布式拓扑结构。Server 作为中央调度节点负责请求编排、知识检索、对话管理等业务逻辑；Agent Client 作为执行节点负责实际的大模型调用、MCP 工具执行等计算密集型任务。两者通过 gRPC 双向流通信，实现了关注点分离和水平扩展。

```mermaid
graph TB
    subgraph Server["Server 中央调度节点"]
        direction TB
        API["REST API :8900"]
        ENGINE["责任链引擎"]
        GRPC_S["gRPC Server :18888"]
        REGISTRY["节点注册表"]
        LB["负载均衡器"]
        HB["心跳监控"]
    end

    subgraph App1["应用 A"]
        C1["Client Node 1<br/>192.168.1.10:18889<br/>maxConcurrent=10"]
        C2["Client Node 2<br/>192.168.1.11:18889<br/>maxConcurrent=20"]
    end

    subgraph App2["应用 B"]
        C3["Client Node 3<br/>192.168.1.20:18889<br/>maxConcurrent=5"]
        C4["Client Node 4<br/>192.168.1.21:18889<br/>maxConcurrent=15"]
    end

    API --> ENGINE
    ENGINE --> GRPC_S
    GRPC_S --> LB
    LB --> C1
    LB --> C2
    LB --> C3
    LB --> C4
    C1 -.->|"心跳 10s"| HB
    C2 -.->|"心跳 10s"| HB
    C3 -.->|"心跳 10s"| HB
    C4 -.->|"心跳 10s"| HB
    HB --> REGISTRY

    style Server fill:#4a90d9,color:#fff
    style App1 fill:#2ecc71,color:#fff
    style App2 fill:#e67e22,color:#fff
```

## Server 与 Client 职责划分

```mermaid
graph LR
    subgraph Server职责["Server 职责"]
        S1["请求接收与鉴权"]
        S2["责任链编排执行"]
        S3["RAG 知识检索"]
        S4["记忆检索与注入"]
        S5["对话管理与存储"]
        S6["系统提示词组装"]
        S7["Trace 追踪记录"]
        S8["节点调度与负载均衡"]
    end

    subgraph Client职责["Client 职责"]
        C1["大模型 API 调用"]
        C2["MCP 工具执行"]
        C3["Advisor 流水线处理"]
        C4["拦截器链执行"]
        C5["流式 Token 回传"]
        C6["工具调用循环 (Agentic Loop)"]
        C7["本地数据访问"]
    end

    style Server职责 fill:#3498db,color:#fff
    style Client职责 fill:#e74c3c,color:#fff
```

| 维度 | Server | Client |
|------|--------|--------|
| **核心定位** | 编排调度 | 执行计算 |
| **部署方式** | 单节点/高可用 | 多节点水平扩展 |
| **数据访问** | 数据库 + 向量库 + 文件存储 | 本地资源 + 外部 API |
| **计算特征** | I/O 密集（数据库查询） | 计算密集（模型调用） |
| **网络通信** | HTTP 入口 + gRPC 出口 | gRPC 入口 + HTTPS 出口（模型API） |
| **扩展方式** | 垂直扩展 | 水平扩展（加节点） |

## gRPC 双向流通信

Server 与 Client 之间通过 gRPC **双向流（Bidirectional Streaming）** 通信，支持大模型流式输出的实时回传。

### 通信流程

```mermaid
sequenceDiagram
    participant User as 用户浏览器
    participant Server as Server (:8900 / :18888)
    participant Client as Client (:18889)
    participant LLM as 大模型 API

    Note over Server,Client: 阶段一：节点注册
    Client->>Server: RegisterNode(appId, hostId, hostIp, grpcPort, maxConcurrent, labels)
    Server-->>Client: RegisterResponse(success, nodeId)

    Note over Server,Client: 阶段二：心跳维持
    loop 每 10 秒
        Client->>Server: Heartbeat(hostId, activeChats)
        Server-->>Client: HeartbeatResponse(ok)
    end

    Note over User,LLM: 阶段三：对话流
    User->>Server: HTTP POST /agent/{id}/chat
    Server->>Server: 责任链执行 (Handler 1-9)
    Server->>Client: gRPC StreamChat(ChatRequest)
    
    Note over Client,LLM: Client 侧 Advisor 流水线
    Client->>Client: Interceptor 前置处理
    Client->>LLM: 调用模型 API (stream=true)
    
    loop 流式输出
        LLM-->>Client: Token chunk
        Client->>Client: Interceptor 后置处理
        Client-->>Server: gRPC StreamResponse(chunk)
        Server-->>User: SSE event: {type:"text", content:"..."}
    end

    LLM-->>Client: [DONE]
    Client-->>Server: gRPC StreamComplete
    Server-->>User: SSE stream end
```

### Proto 定义概览

```protobuf
service AgentService {
  // 节点注册
  rpc RegisterNode(RegisterRequest) returns (RegisterResponse);
  
  // 心跳上报
  rpc Heartbeat(HeartbeatRequest) returns (HeartbeatResponse);
  
  // 双向流对话
  rpc StreamChat(ChatRequest) returns (stream ChatResponse);
}

message ChatRequest {
  string conversationId = 1;
  string model = 2;
  repeated Message messages = 3;
  repeated ToolDefinition tools = 4;
  ModelParameters parameters = 5;
}

message ChatResponse {
  string type = 1;      // "text" | "thinking" | "tool_call" | "error" | "done"
  string content = 2;
  ToolCallInfo toolCall = 3;
}
```

## 节点注册与心跳

### 注册流程

```mermaid
stateDiagram-v2
    [*] --> Starting: Client 启动
    Starting --> Registering: 初始化完成
    Registering --> Online: 注册成功
    Registering --> RetryRegister: 注册失败
    RetryRegister --> Registering: 重试 (3s 间隔)
    Online --> Heartbeating: 进入心跳循环
    Heartbeating --> Online: 心跳成功
    Heartbeating --> Reconnecting: 心跳失败
    Reconnecting --> Registering: 重新注册
    Online --> Offline: 主动下线
    Offline --> [*]
```

Client 节点启动后，向 Server 发送注册请求，携带以下信息：

| 注册字段 | 说明 | 示例 |
|----------|------|------|
| `appId` | 所属应用 ID | `order-ai-app` |
| `hostId` | 节点唯一标识 | `node-001` |
| `hostIp` | 节点 IP 地址 | `192.168.1.10` |
| `grpcPort` | Client gRPC 端口 | `18889` |
| `maxConcurrent` | 最大并发对话数 | `10` |
| `labels` | 节点标签（用于路由） | `{"gpu":"A100", "region":"east"}` |

### 心跳机制

```mermaid
sequenceDiagram
    participant Client as Client Node
    participant Server as Server
    participant Registry as 节点注册表

    loop 每 10 秒
        Client->>Server: Heartbeat(hostId, activeChats, timestamp)
        Server->>Registry: 更新 expireDt = now + 30s
        Server-->>Client: HeartbeatResponse(ok)
    end

    Note over Server,Registry: 如果 30s 未收到心跳
    Server->>Registry: 标记节点 offline
    Registry->>Registry: 从可用节点列表移除
```

**关键参数：**

| 参数 | 值 | 说明 |
|------|-----|------|
| 心跳间隔 | 10 秒 | Client 每 10 秒发送一次心跳 |
| 过期时间 | 30 秒 | 超过 30 秒未收到心跳，Server 标记节点离线 |
| 重试策略 | 3 次/3s | 心跳失败重试 3 次，间隔 3 秒 |
| 重新注册 | 自动 | 重试耗尽后自动触发重新注册流程 |

### 节点生命周期

```mermaid
stateDiagram-v2
    [*] --> REGISTERING: Client 启动
    REGISTERING --> ONLINE: 注册成功
    ONLINE --> ONLINE: 心跳正常 (每10s)
    ONLINE --> EXPIRING: 心跳超时 (>30s)
    EXPIRING --> ONLINE: 心跳恢复
    EXPIRING --> OFFLINE: 超时确认 (>60s)
    ONLINE --> OFFLINE: 主动注销
    OFFLINE --> REGISTERING: 重新连接
    OFFLINE --> [*]: 节点终止

    note right of ONLINE
        可接收调度任务
        activeChats < maxConcurrent
    end note

    note right of EXPIRING
        暂停调度新任务
        等待心跳恢复
    end note

    note right of OFFLINE
        从注册表移除
        存量任务等待超时
    end note
```

## 负载均衡策略

Snail AI 提供 **6 种负载均衡策略**，可按应用（App）级别配置：

```mermaid
graph TD
    REQ["对话请求"] --> LB["负载均衡器"]
    LB --> D{"路由策略?"}

    D -->|RANDOM| R1["随机路由"]
    D -->|ROUND_ROBIN| R2["轮询路由"]
    D -->|LEAST_ACTIVE| R3["最少活跃"]
    D -->|CONSISTENT_HASH| R4["一致性哈希"]
    D -->|WEIGHTED| R5["加权随机"]
    D -->|DESIGNATED| R6["指定节点"]

    R1 --> FILTER["可用节点过滤"]
    R2 --> FILTER
    R3 --> FILTER
    R4 --> FILTER
    R5 --> FILTER
    R6 --> FILTER

    FILTER --> SELECT["选中节点"]
    FILTER --> FALLBACK["无可用节点 → 降级"]

    style REQ fill:#3498db,color:#fff
    style LB fill:#e74c3c,color:#fff
    style SELECT fill:#2ecc71,color:#fff
    style FALLBACK fill:#95a5a6,color:#fff
```

### 策略详解

| 策略 | 标识 | 算法 | 适用场景 |
|------|------|------|----------|
| **随机路由** | `RANDOM` | 从可用节点中随机选取 | 节点能力均等，通用场景 |
| **轮询路由** | `ROUND_ROBIN` | 按序依次分配 | 请求量均匀分散 |
| **最少活跃** | `LEAST_ACTIVE` | 选择当前 `activeChats` 最少的节点 | 负载敏感型，推荐默认 |
| **一致性哈希** | `CONSISTENT_HASH` | 基于 `conversationId` 哈希选择 | 同一对话固定到同一节点 |
| **加权随机** | `WEIGHTED` | 按 `maxConcurrent` 权重加权随机 | 异构节点（GPU 算力不同） |
| **指定节点** | `DESIGNATED` | 根据 Agent 配置路由到指定节点 | 特定模型仅部署在特定节点 |

### 策略选择决策图

```mermaid
flowchart TD
    START["需要选择路由策略"] --> Q1{"节点算力是否均等?"}
    Q1 -->|是| Q2{"是否需要会话亲和性?"}
    Q1 -->|否| Q3{"是否有明确权重配置?"}

    Q2 -->|是| HASH["一致性哈希<br/>CONSISTENT_HASH"]
    Q2 -->|否| Q4{"是否关注实时负载?"}

    Q4 -->|是| LEAST["最少活跃<br/>LEAST_ACTIVE<br/>(推荐)"]
    Q4 -->|否| Q5{"需要均匀分配?"}

    Q5 -->|是| RR["轮询路由<br/>ROUND_ROBIN"]
    Q5 -->|否| RAND["随机路由<br/>RANDOM"]

    Q3 -->|是| WEIGHTED["加权随机<br/>WEIGHTED"]
    Q3 -->|否| Q6{"是否需要固定路由?"}

    Q6 -->|是| DESIGNATED["指定节点<br/>DESIGNATED"]
    Q6 -->|否| LEAST

    style LEAST fill:#2ecc71,color:#fff
    style HASH fill:#3498db,color:#fff
    style WEIGHTED fill:#f39c12,color:#fff
    style DESIGNATED fill:#e74c3c,color:#fff
    style RR fill:#9b59b6,color:#fff
    style RAND fill:#95a5a6,color:#fff
```

## 水平扩展模式

### 单 Server + 多 Client

最常见的部署模式，适合中小规模场景：

```mermaid
graph TB
    subgraph Server
        S["Server 节点<br/>8900 / 18888"]
    end

    subgraph ClientCluster["Client 集群"]
        C1["Client 1<br/>高配节点<br/>模型: OpenAI-compatible Chat"]
        C2["Client 2<br/>内网节点<br/>模型: OpenAI-compatible Chat"]
        C3["Client 3<br/>CPU Only<br/>模型: OpenAI-compatible Chat"]
    end

    S --> C1
    S --> C2
    S --> C3

    style Server fill:#4a90d9,color:#fff
    style ClientCluster fill:#2ecc71,color:#fff
```

### 多应用隔离模式

不同业务线使用独立的 Client 集群，通过 App 实现资源隔离：

```mermaid
graph TB
    subgraph Server
        S["Server 节点"]
    end

    subgraph AppA["App: 客服系统"]
        A1["Client A-1<br/>maxConcurrent=50"]
        A2["Client A-2<br/>maxConcurrent=50"]
    end

    subgraph AppB["App: 内部问答"]
        B1["Client B-1<br/>maxConcurrent=10"]
    end

    subgraph AppC["App: 数据分析"]
        C1["Client C-1<br/>GPU: A100<br/>maxConcurrent=5"]
    end

    S -->|"ROUND_ROBIN"| AppA
    S -->|"RANDOM"| AppB
    S -->|"DESIGNATED"| AppC

    style Server fill:#4a90d9,color:#fff
    style AppA fill:#2ecc71,color:#fff
    style AppB fill:#f39c12,color:#fff
    style AppC fill:#e74c3c,color:#fff
```

## 故障容错

### 故障场景与处理

```mermaid
flowchart TD
    REQ["对话请求到达"] --> SELECT["选择 Client 节点"]
    SELECT --> CALL["gRPC 调用"]
    CALL --> CHECK{"调用是否成功?"}

    CHECK -->|成功| STREAM["流式回传"]
    CHECK -->|连接失败| MARK_FAIL["标记节点异常"]
    CHECK -->|超时| TIMEOUT["超时处理"]

    MARK_FAIL --> RETRY{"剩余可用节点?"}
    TIMEOUT --> RETRY

    RETRY -->|是| FAILOVER["故障转移<br/>选择下一个节点"]
    RETRY -->|否| ERROR["返回错误<br/>'无可用执行节点'"]

    FAILOVER --> CALL

    STREAM --> DONE["完成"]

    style DONE fill:#2ecc71,color:#fff
    style ERROR fill:#e74c3c,color:#fff
    style FAILOVER fill:#f39c12,color:#fff
```

| 故障场景 | 处理策略 | 说明 |
|----------|----------|------|
| Client 节点宕机 | 心跳超时自动摘除 | 30s 无心跳标记离线，不再分配新任务 |
| gRPC 连接失败 | 故障转移到其他节点 | 自动选择下一个可用节点重试 |
| 模型调用超时 | 超时返回错误 | 可配置超时时间，默认 120s |
| Client 过载 | 负载均衡规避 | `LEAST_ACTIVE` 策略自动规避高负载节点 |
| Server 重启 | Client 自动重新注册 | Client 检测到连接断开后触发重新注册 |

### 优雅下线

```mermaid
sequenceDiagram
    participant Ops as 运维人员
    participant Client as Client Node
    participant Server as Server

    Ops->>Client: 发送 SIGTERM
    Client->>Client: 设置 shutdownFlag = true
    Client->>Server: Deregister(hostId)
    Server->>Server: 从注册表移除节点
    Server-->>Client: DeregisterResponse(ok)
    
    Note over Client: 等待存量对话完成 (gracePeriod=30s)
    Client->>Client: 所有对话结束 / 超时
    Client->>Client: 关闭 gRPC Server
    Client->>Client: 进程退出
```

## 多节点部署配置

### Server 端配置

```yaml
# application.yml - Server 配置
snail-ai:
  grpc:
    server:
      port: 18888
      # 节点过期时间（秒），超过此时间未收到心跳标记离线
      node-expire-seconds: 30
      # 最大消息大小
      max-message-size: 16MB
```

### Client 端配置

```yaml
# application.yml - Client 配置
snail-ai:
  client:
    # 所属应用 ID（需在 Server 管理后台预先创建）
    app-id: my-ai-app
    # 节点唯一标识（同一 App 下不可重复）
    host-id: node-001
    # Server gRPC 地址
    server-address: 192.168.1.100:18888
    grpc:
      port: 18889
    # 最大并发对话数
    max-concurrent: 10
    # 心跳间隔（秒）
    heartbeat-interval: 10
    # 节点标签（可用于路由决策）
    labels:
      gpu: A100
      region: east
      env: production
```

### Docker Compose 多节点示例

```yaml
version: '3.8'
services:
  server:
    image: snail-ai/server:latest
    ports:
      - "8900:8900"
      - "18888:18888"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/snail_ai

  client-1:
    image: snail-ai/client:latest
    environment:
      - SNAIL_AI_CLIENT_APP_ID=default
      - SNAIL_AI_CLIENT_HOST_ID=client-1
      - SNAIL_AI_CLIENT_SERVER_ADDRESS=server:18888
      - SNAIL_AI_CLIENT_MAX_CONCURRENT=10

  client-2:
    image: snail-ai/client:latest
    environment:
      - SNAIL_AI_CLIENT_APP_ID=default
      - SNAIL_AI_CLIENT_HOST_ID=client-2
      - SNAIL_AI_CLIENT_SERVER_ADDRESS=server:18888
      - SNAIL_AI_CLIENT_MAX_CONCURRENT=20

  client-3:
    image: snail-ai/client:latest
    environment:
      - SNAIL_AI_CLIENT_APP_ID=high-perf
      - SNAIL_AI_CLIENT_HOST_ID=client-3
      - SNAIL_AI_CLIENT_SERVER_ADDRESS=server:18888
      - SNAIL_AI_CLIENT_MAX_CONCURRENT=5
      - SNAIL_AI_CLIENT_LABELS_GPU=A100
```
