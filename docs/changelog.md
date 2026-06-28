# 更新日志

本页记录 Snail AI 各版本的变更内容，遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/) 格式。

版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：`主版本号.次版本号.修订号`

- **主版本号** -- 包含不兼容的 API 变更
- **次版本号** -- 新增功能（向下兼容）
- **修订号** -- Bug 修复（向下兼容）

---

## [1.0.0] - 2026-06-28

### 版本亮点
- 发布首个 1.0 正式版本，围绕开源版可落地能力完成文档、部署、数据库和智能体对话链路整理。
- 新增独立「智能体对话」能力，提供 `/snail-chat` 页面、会话 Token、嵌入配置和前后端源码说明。
- 完善 RAG、智能体、技能、模型、智能体对话等核心页面的实际界面截图，提升文档可读性和上手体验。

### 新增功能
- 新增 VitePress 文档站点与文档打包脚本，文档源码、Docker、SQL、部署说明统一迁移到 `docs/` 目录。
- 新增文档维护智能体规范，明确开源版文档必须以当前源码、配置、SQL 和脚本为准。
- 新增达梦数据库支持，包含 `snail-ai-dm-storage` 存储模块、Mapper 适配、依赖声明和 `docs/sql/snail_ai_schema_dameng.sql` 初始化脚本。
- 新增 GitNexus 代码智能规范与相关 Skill 文档，用于代码影响分析、调试、重构和索引维护。
- 智能体对话新增独立前端 `snail-ai-chat` 与后端 `snail-ai-agent-chat` 文档说明，前端构建产物可嵌入 Starter 静态资源目录。

### 功能优化
- 优化 Docker Compose 一键启动配置，统一依赖服务、初始化脚本和示例应用构建路径。
- 优化 MCP 服务连接测试，`testConnection` 仅返回本次连接结果 `true/false`，不再把连接状态作为持久状态维护。
- 删除 MCP 连接状态相关枚举、VO 字段、持久化字段和查询条件，避免把瞬时连接结果展示为长期状态。
- 优化 Chat 与 OpenAPI 的启用开关，智能体对话 Starter 复用 OpenAPI 客户端能力自动装配。
- 优化向量存储缓存刷新逻辑，缓存判断同时考虑模型配置和向量库存储实例更新时间，修复向量库配置变更后缓存不刷新的问题。
- 优化 RAG 处理链路中文档、切片、检索、问答等页面说明，补充当前实际 UI 的使用路径。

### 文档与站点
- 新增并校验 Admin API、OpenAPI、部署、架构、RAG、智能体、模型、MCP、Skill、资源、用户、记忆等文档页面。
- 移除开源版未实现功能的完成态描述，包括独立 Admin Memory API、全局 Dashboard、Trace/Score/Langfuse 风格观测、独立 OpenAPI RAG 端点等。
- 修正认证说明：OpenAPI 外部集成使用 `Snail-Ai-App-Id` 与 `Snail-Ai-Token`；`Snail-Ai-Auth` 仅用于 Admin API 或智能体对话会话。
- 「客户端 Chat 模式」更名为「智能体对话」，并移动到智能体菜单下。
- 新增智能体对话、技能管理、智能体列表/配置、模型管理、RAG 列表/创建/文档/切片/检索/问答等截图。

### 问题修复
- 修复向量存储实例缓存不随配置更新时间失效的问题。
- 修复 MCP 工具解析中依赖连接状态导致状态不准确的问题。
- 修复 Docker Compose 和示例 Dockerfile 在一键启动场景下的路径与初始化问题。
- 修复文档中旧路径、旧端口、旧认证方式和未实现功能描述与当前开源源码不一致的问题。

### 升级说明
- 如果历史数据库中存在 MCP 连接状态字段，需要按新版 SQL 删除相关字段并以实时连接测试结果为准。
- 文档相关脚本和资源路径已从 `script/` 迁移到 `docs/`，部署与打包请使用 `docs/docker/`、`docs/sql/` 和 `docs/package-docs.sh`。
- 如需使用达梦数据库，请使用新增的达梦存储模块与 `docs/sql/snail_ai_schema_dameng.sql` 初始化脚本。
- 原文档路径 `/guide/client/chat` 已调整为 `/guide/agent/conversation`。

## [0.0.6] - 2026-06-22

### 新增功能
- 支持业务库 MySQL / PostgreSQL 多数据库适配，新增 `snail-ai-postgres-storage` 模块与 PostgreSQL 全量建表脚本。
- 新增统一资源访问入口 `/files/**`，支持本地/MinIO 资源在线预览与下载。
- 对话记录新增 Token 用量采集与持久化，支持输入 Token、输出 Token、缓存命中 Token 统计。
- Chat UI 新增嵌入模式配置，支持控制顶部栏、用户侧栏、智能体市场、紧凑输入框和智能体锁定。
- OpenAPI 支持清空指定智能体下的全部会话记录。
- 用户体系新增昵称、头像资源绑定能力，OpenAPI 用户注册支持外部头像 URL。
- 新增自定义 ChatClient 扩展点示例，演示工具调用上下文透传。

### 功能优化
- 优化资源管理链路，资源访问改为基于 `storageKey` 的公开访问 URL，减少对资源自增 ID 的直接暴露。
- 优化 RAG 文档预览/下载返回结构，新增 `previewUrl` 与 `downloadUrl`。
- 优化短期记忆查询顺序，修复上下文窗口取数后顺序不正确的问题。
- 优化模型密钥回显逻辑，编辑模型配置时可正确处理已有密钥。
- 重排模型模块调整：`snail-ai-model-rerank-provider-http` 更名为 `snail-ai-model-rerank-provider-qwen`。
- OpenAI 兼容模型流式合并保留 `reasoning_content` 等扩展字段，提升思考过程兼容性。

### 问题修复
- 修复智能体对话思考过程不显示的问题。
- 修复短期记忆上下文未生效的问题。
- 修复 PostgreSQL 场景下部分分页、Mapper SQL 与建表兼容问题。
- 修复资源存储 Key 中业务类型、文件扩展名存在非法字符时的访问问题。

### snail-ai-admin
- 资源管理页支持 Markdown、Word、Excel 文件抽屉预览，并支持直接下载。
- 新增鉴权图片/头像组件，解决头像、资源图片在鉴权 URL 下显示不稳定的问题。
- 系统用户管理支持头像展示、头像选择和恢复默认头像。
- 智能体详情页优化思考过程展示样式，并默认展开思考内容。
- 移除管理端独立 `/chat` 页面与入口，登录后统一进入首页。
- 首页智能体排行、最近会话、全局头部头像等位置统一使用资源头像展示。

### 升级说明
- 使用 PostgreSQL 的部署需要切换到新增的 PostgreSQL 建表脚本与存储模块。
- 数据库表结构涉及 `sai_user`、`sai_agent_conversation_record` 等字段变更，升级前建议先备份数据库。
- 依赖 `snail-ai-model-rerank-provider-http` 的项目需要改为 `snail-ai-model-rerank-provider-qwen`。

## [0.0.5] - 2026-06-18

### 核心升级

- **Spring AI 升级至 2.0.0**：从 M8 升级 正式候选版本，全面适配新版 API

### 架构重构

- **整体结构重构**：对项目模块进行大规模重构，优化模块分层和职责划分
- **SSE 替换为 Flux**：删除 SSE（Server-Sent Events）实现，统一改用 Reactor Flux 响应式流，提升流式对话的稳定性和性能
- **模型适配层重构**：新增 `ServerModelSpecFactory`，统一服务端模型配置到模型构建规格的适配逻辑，支持 Chat / Embedding / Rerank 三类模型规格
- **模型类型重排**：优化 `ModelTypeEnum` 等模型类型定义的排列和分类

### 新功能

- **智能体对话**：新增独立前端 `snail-ai-chat` 与后端模块 `snail-ai-agent-chat`，包含：
  - `snail-ai-agent-chat-api`：Chat 会话、认证、凭证校验等 API 定义
  - `snail-ai-agent-chat-starter`：自动装配、网关控制器、Token 服务、认证拦截器
  - 前端构建产物嵌入 Starter 静态资源目录，支持 `/snail-chat` 路径访问
- **OpenAPI 客户端模块** (`snail-ai-agent-openapi`)：新增面向外部调用的 OpenAPI 模块，包含：
  - Agent 客户端、Embed 客户端接口
  - 智能体对话 Token 请求/响应 DTO
  - OpenAPI 认证拦截器与会话管理
- **gRPC 通信层**：新增 `ClientGrpcServer`、`GrpcChannelProvider`、`GrpcClientInvokeHandler` 等 gRPC 组件及 `ChatDispatchStreamingHandler` 流式分发处理器
- **记忆系统**：新增对话记忆（`ConversationMemory`）相关 Mapper 和 PO，支持记忆注入 Advisor（`MemoryInjectionAdvisor`）
- **初始化测试数据**：新增初始化测试应用和测试 OpenID，方便开箱即用

### 优化

- **Chat 路径调整**：将 `/chat` 路径替换为 `/snail-chat`，避免与业务路径冲突
- **浏览器图标**：新增 favicon 显示项目 Logo
- **滑动窗口限流**：新增 `SlidingRingWindow` 滑动环形窗口组件和 `ActiveChatCounter` 活跃对话计数器
- **客户端路由策略**：新增一致性哈希路由（`ConsistentHashClientRoute`）等客户端路由策略

### Bug 修复

- 修复 Chat 相关 Bug 并重新打包
- 修复 Server 端 UI 打包问题

### 包含的 0.0.4 变更

- 升级 Spring AI 2.0.0
- 修复使用千问模型调用 API 时参数错误
- 修复 model 覆盖问题
- 优化 ES HTTPS 模式
- 修复 shellTool 执行失败问题
- 智能体创建支持前置选择执行应用
- 智能体支持多选 RAG
- 智能体支持选择强制调用/智能调用
- RAG 支持批量删除

## [0.0.4] - 2026-06-08

### 核心更新

- 优化智能体创建流程，支持创建前选择执行应用。
- 智能体支持多选 RAG。
- 智能体支持选择工具调用模式：强制调用 / 智能调用。
- RAG 支持批量删除。

### 问题修复

- 修复使用千问模型调用 API 时参数错误的问题。
- 修复模型配置被覆盖的问题。
- 修复 ShellTool 执行失败的问题。
- 修复 `snail-ai-admin` 预览异常问题。

### 优化调整

- 优化 Elasticsearch HTTPS 模式支持。


## [0.0.3] - 2026-06-01

### 核心升级

- **Spring AI 升级至 2.0.0-M8**：依次完成 M7、M8 版本适配
    - `OpenAiChatModelFactory` 改为 `OpenAiChatOptions` + `OpenAiChatModel.builder()` 新版 API
    - `OpenAiEmbeddingModelFactory` 改为 M7/M8 的 `OpenAiEmbeddingOptions` 构造方式，修正 `encodingFormat` 枚举类型
    - `ClientChatExecutor` 改用 `ChatClient.defaultTools(...).advisor(ToolCallAdvisor...)` 方式注册工具
    - Spring AI 2.x 不再支持自定义 `embeddingsPath` 和 `completionsPath`，移除相关配置字段
- **Spring Boot 升级至 4.0.6**，同步升级 langchain4j、volcengine、milvus、minio、okhttp 等依赖
- **依赖版本统一管理**：将各模块依赖版本归拢到根 POM `properties`，示例工程改用 Spring Boot BOM

### 功能修复

- 修复 Tool 重复调用问题：移除 `MemoryInjectionAdvisor`，优化 `ThinkingCollectorAdvisor` 逻辑
- 修复思考过程（Thinking）不显示问题
- 修复 RAG 智能切分文档过大导致请求 tokens 超限，增加分段处理逻辑
- 修复 RAG 切分时 `overlap` 未设置默认值导致空指针报错
- 修复关闭客户端时自动装配模块启动报错（`SnailAiAgentProperties` 移除 `@Configuration`，改由 `AutoConfiguration` 统一注册）
- 修复 RAG 上传提交使用临时 `String` 加锁导致并发保护失效，改为 `ConcurrentHashMap` 稳定锁对象

### 安全加固

- **资源访问权限**：预览和下载接口增加 `@LoginRequired` 校验，普通用户只能访问自己的资源，管理员可访问全部
- **密码安全升级**：用户密码摘要从固定盐 SHA-256 升级为 PBKDF2WithHmacSHA256（120,000 次迭代），登录时自动迁移老密码
- **Agent 工具安全边界**：
    - `ShellTool` 限制工作目录只能位于技能目录内，修正命令超时处理
    - `HttpTool` 禁止访问本机/内网地址、禁止覆盖 Host 头，移除请求体日志
- **本地资源存储**增加根目录约束，防止 `storageKey` 路径穿越
- **URL 导入**禁止内网/localhost 地址，限制下载文件大小
- **OpenAPI/gRPC 认证**使用常量时间比较校验 app token，防止时序攻击
- **RAG 文档处理**使用条件状态更新抢占待处理文档，避免多实例重复处理；向量库写入成功后再回写 chunk vectorId
- **Agent 用量统计**改为原子自增，降低并发计数丢失风险

### 优化改进

- **全面启用虚拟线程**：移除自定义 `AsyncConfig` 线程池，`CompletableFuture.runAsync` 替换为 `Thread.startVirtualThread`，通过 `spring.threads.virtual.enabled=true` 全局启用
- **自动装配优化**：`SnailAiOpenApiAutoConfiguration` 改进条件装配逻辑，`OpenApiDemoController` 增加 `@ConditionalOnProperty` 条件控制
- **gRPC keepAlive 调整**：`permitKeepAliveTime` 从 5 分钟缩短为 30 秒，减少长连接空闲断开
- **合并重复 Maven 插件声明**：`snail-ai-starter/pom.xml` 中 `spring-boot-maven-plugin` 去重
- **适配 MinIO 9.x API 变更**，修复 Milvus 健康检查对 Hadoop 私有 proto 的错误依赖

### 不兼容变更

- 数据库表名缩短：`snail_ai_*` 统一改为 `sai_*`，**升级需执行新版 SQL 脚本**
- 服务端口从 `8080` 改为 `8900`
- 默认用户密码改为 `admin123`
- 模型配置 `apiEndpoint` 字段改为必填（增加 `@NotBlank` 校验）
- 移除模型配置中的 `completionsPath` 和 `embeddingsPath` 扩展字段

---

## [0.0.2] - 2026-05-24

### 新增

- 客户端自定义 Tool 支持（`CustomToolCallbackProvider`），可通过配置注册自定义工具回调扩展智能体能力
- RAG 搜索工具（`RagSearchTool` + `RagSearchCallbackHandler`），智能体对话中自动调用知识检索
- 向量维度约束服务（`VectorDimensionConstraintService`），知识库向量维度校验和管理
- `QueryDbTool` 示例，演示客户端如何自定义数据库查询工具

### 优化

- 向量数据库兼容性优化（`VectorStoreFactory` 逻辑调整）
- `ClientRagToolResolver` 增强，支持更灵活的 RAG 工具解析
- gRPC `ChatDispatchStreamingHandler` 流式调度逻辑优化
- 前端 RAG 页面交互增强（向量维度约束展示、存储实例关联）
- 前端模型配置面板优化（`ConfigPanel.vue` 重构）

### 升级说明

- 项目版本号从 `0.0.1` 升级至 `0.0.2`
- 知识库相关接口新增 `vectorDimension` 字段，需同步更新数据库（参见 `script/sql/snail_ai_schema.sql`）
- 客户端如需自定义 Tool，参考 `snail-ai-agent-example` 中的 `QueryDbTool` 示例

---

## [0.0.1] - 2025-05-01

### 新增

#### 智能体管理
- AI 辅助流式创建智能体（基于自然语言描述自动生成配置）
- 手动创建和模板快速创建
- 智能体详情页：编辑配置、数据分析等标签页
- 系统提示词配置、预设问题设置、问候语自定义
- 智能体市场：发布与订阅机制
- 企业精选推荐功能

#### 对话系统
- 流式对话输出（SSE）
- 思维链（Chain of Thought）展示
- 对话历史管理
- 多轮上下文保持

#### Agent 责任链架构
- 10 个 Handler 的完整责任链流水线
- Init -> ModelResolve -> SystemPrompt -> Conversation -> Mcp -> Rag -> Skill -> WebSearch -> ContextCollector -> LlmCall
- 各 Handler 可独立扩展和替换

#### 客户端模式（Agent Client）
- gRPC 双向流 Server-Agent 分布式架构
- 拦截器机制（SnailAiInterceptor SPI）
- 5 级 Advisor 处理流水线
- 本地工具执行（Shell / HTTP / MCP）
- `@EnableSnailAiAgent` 一键启用注解
- 在线日志实时查看

#### 多模型支持
- 模型提供商管理（OpenAI、Claude、Ollama、Gemini、火山引擎）
- 五种模型类型统一管理：CHAT / EMBEDDING / RERANKER / IMAGE / SPEECH
- 模型作用域：GLOBAL（全局）/ PERSONAL（个人）
- 默认模型设置
- 使用统计

#### RAG 知识库
- 支持 10+ 文档格式（PDF、Word、Excel、PPT、Markdown、TXT、HTML、CSV 等）
- 4 种分片策略：固定长度、递归字符、Token 级别、语义分片
- 混合检索：向量检索 + BM25 + RRF / 加权融合
- Reranker 重排序支持
- 文档智能去重（基于内容哈希）
- 存储实例管理（PgVector / Milvus / Elasticsearch）

#### MCP 工具集成
- 完整 MCP 协议实现
- 三种传输方式：SSE、Streamable HTTP、Stdio
- 四种认证方式：API Key、Bearer Token、OAuth 2.0、无认证
- 一键连接测试
- 工具发现与注册

#### 技能系统
- ZIP 包方式上传技能
- SKILL.md 技能定义文件
- 技能绑定与管理

#### 记忆系统
- 短期记忆（滑动窗口）
- memory / db 两种存储模式

#### 应用与分布式
- 应用管理（多 Client 节点编排）
- 路由策略：随机、轮询、指定节点
- 客户端节点状态监控
- 心跳检测与自动故障转移

#### 联网搜索
- Web Search 集成
- 实时互联网信息检索

#### 多数据库支持
- MySQL(已实现)

#### 向量存储
- PgVector
- Milvus
- Elasticsearch

#### 文件存储
- 本地文件系统
- MinIO 对象存储

#### 其他
- 资源管理（图片库）
- 用户管理与权限控制
- OpenAPI 外部集成接口（认证 + 对话）

---

## 变更类型说明

| 类型 | 说明 |
|------|------|
| **新增** (Added) | 新功能 |
| **变更** (Changed) | 对现有功能的变更 |
| **废弃** (Deprecated) | 即将移除的功能 |
| **移除** (Removed) | 已移除的功能 |
| **修复** (Fixed) | Bug 修复 |
| **安全** (Security) | 安全相关的修复 |
| **Breaking Change** | 不兼容的变更，升级时需额外处理 |

---

[未发布]: https://gitee.com/opensnail/snail-ai/compare/v0.0.2...HEAD
[0.0.2]: https://gitee.com/aizuda/snail-ai/compare/v0.0.1...v0.0.2
[0.0.1]: https://gitee.com/aizuda/snail-ai/releases/tag/v0.0.1
