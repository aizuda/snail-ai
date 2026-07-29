# 更新日志

本页记录 Snail AI 各版本的变更内容，遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/) 格式。

版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：`主版本号.次版本号.修订号`

- **主版本号** -- 包含不兼容的 API 变更
- **次版本号** -- 新增功能（向下兼容）
- **修订号** -- Bug 修复（向下兼容）

---

## [1.1.1] - 2026-07-29

### 版本亮点
- 升级内嵌智能体对话前端，补充空状态提示和智能体市场入口，优化用户进入对话与切换智能体的体验。
- 优化 OpenAPI 智能体列表过滤逻辑，统一从当前 OpenAPI 会话上下文获取 `appId`，避免客户端透传过滤条件带来的不一致。
- 梳理 Docker Compose 与 CI 初始化链路，提升演示环境、源码构建和 Alibaba Cloud Linux 部署兼容性。

### 新增功能
- 内嵌聊天页静态资源升级到 1.1.1，对话模块新增空状态展示和智能体市场入口。
- `.env.example` 补充 Docker Compose 源码构建路径、数据源、加密密钥和 Agent 连接配置示例。

### 功能优化
- 智能体列表查询改为服务端基于 `OpenApiSessionUtils.current().getAppId()` 过滤，并同时返回当前应用绑定智能体和公共智能体。
- 会话 Token 解析在 JWT 缺少 `appId` 时回退到 Agent 本地配置，提升旧 Token 或简化配置场景的兼容性。
- Docker Compose 构建上下文改为通过 `SNAIL_AI_STARTER_PATH`、`SNAIL_AI_AGENT_EXAMPLE_PATH` 配置，便于不同目录结构下复用部署脚本。
- Server 与 Agent 示例容器启动命令统一支持 `JAVA_OPTS` 注入，便于通过 JVM 参数覆盖数据源、加密密钥和 Agent 连接配置。
- Maven 构建配置切换为华为云 Maven 镜像，并保留 Spring Milestones 仓库，减少不必要的快照仓库配置。

### 问题修复
- `ci-setup.sh` 新增 `alinux` 发行版识别，JDK、Maven、Git、Node.js 安装流程适配 Alibaba Cloud Linux。
- 新增 `.gitattributes` 强制 Shell 脚本使用 LF 换行，修复 CRLF 导致 Linux 环境脚本无法直接执行的问题。

### 部署与文档
- Docker 部署文档补充容器内初始化 MySQL 数据库的命令示例。
- Docker 部署文档补充 Ollama 演示模型手动拉取说明，包含 Embedding 模型和轻量聊天模型示例。
- 部署脚本移除内置 `init-db` 命令，数据库初始化改为按文档手动执行。
- 移除冗余的 `script/deploy/init-db.sh` 与 `script/pull-models.sh`，简化部署脚本职责。

### 升级说明
- 使用 Docker Compose 部署时，请检查 `.env` 或运行环境中的源码构建路径、数据库连接、加密密钥和 Agent 连接参数是否与 1.1.1 模板一致。
- 如果此前依赖 `script/deploy/init-db.sh` 或 `script/pull-models.sh`，升级后请改用 `docs/deploy/docker.md` 中的数据库初始化和 Ollama 模型拉取命令。
- 如果在 Alibaba Cloud Linux 或 Windows 编辑后上传脚本的环境中部署，建议更新到新版 `script/deploy/ci-setup.sh`，并确保 Shell 脚本保持 LF 换行。

## [1.1.0] - 2026-07-28

### 新增功能
- 智能体对话支持图片附件链路，独立聊天页、OpenAPI 和 Agent SDK 可上传图片并随消息发送，支持图片消息预览、附件校验与会话记录元数据持久化。
- 模型配置新增能力标记，CHAT 模型可开启 `vision` 支持图片输入，并可配置默认视觉模型用于图片识别和 RAG 图片 OCR 兜底。
- RAG 知识库新增 Docling 解析引擎，支持 PDF/文档版面元素解析、Markdown 内容留存、解析统计和解析引擎参数回显。
- RAG 图片识别能力增强，支持 Docling 图片提取、PaddleOCR/多模态模型 OCR、图片 chunk 构建、图片证据召回与检索结果图片证据返回。
- 模型配置新增连通性验证，支持 Chat、Embedding、Reranker 三类模型测试，并完善模型用量统计持久化。
- 首页新增运营概览聚合接口，支持 KPI、趋势、排行、知识库、技能与资源数据汇总，Token 趋势改为按对话记录读时聚合。
- OpenAPI 增加会话标题更新、资源上传、用户解析等能力，图片对话与资源返回结构同步扩展。
- MCP 服务配置支持超时时间和请求头配置，便于对接需要自定义 Header 或较长响应时间的 MCP 服务。
- 新增 Docling API 与 PaddleOCR API 工具目录及 Docker 化部署支持，补充 `.env.example`、Docker Compose 与相关启动脚本。

### 功能优化
- 拆分 Agent、模型配置、RAG 文档、Skill 等管理端大服务类，补充 Assembler、Validator、QueryService、FileService 等职责边界，降低服务复杂度。
- 优化智能体对话链路，新增附件解析处理，调整模型视觉能力分发、Prompt 构建、RAG/Skill/MCP 工具解析和流式输出元数据。
- 优化 RAG 文档导入与检索流水线，支持图片 OCR 质量评估、图片与文本 chunk 匹配、检索结果重排和图片证据增强。
- 优化客户端实例管理和心跳调度，补充客户端踢出处理、路由策略可观测性和异常路径日志。
- 完善资源、向量存储、全文检索、OpenAPI、鉴权、异常处理、RAG 和 Agent 链路的观测性日志与公开 API 注释。
- 接入 EditorConfig、Spotless、commit-msg 钩子和 git-submit/frontend-dev/env-config 技能，统一代码格式、提交规范和开发流程说明。

### 问题修复
- 修复 Docling v2 API 兼容、图片 caption 前瞻提取、页码识别和 Media 构造方式等问题。
- 修复 RAG 文档管道资源引用、图片 OCR/图注/公式内容不可检索、知识库编辑时解析配置未保留等问题。
- 修复批量上传超时，增加上传预览耗时日志和 axios 超时配置。
- 修复首页模型用量统计为空、首页 Token 趋势统计口径不准确的问题。
- 修复 `AdminWebController` 与 Agent Chat `WebController` Bean 名称冲突。
- 修复模型配置字段长度校验不足导致插入异常，以及删除模型/存储/MCP 时缺少引用检查的问题。
- 修复图片对话执行端依赖版本、模型视觉能力返回和图片附件校验链路中的兼容问题。

### 数据库变更
- MySQL、PostgreSQL、达梦三套建表脚本同步更新：`docs/sql/snail_ai_schema.sql`、`docs/sql/snail_ai_schema_pgsql.sql`、`docs/sql/snail_ai_schema_dameng.sql`。
- `sai_agent_conversation_record` 新增 `metadata` 字段，用于保存图片附件、工具结果等消息扩展元数据。
- `sai_rag_document` 新增 Docling 解析统计与内容字段：`page_count`、`element_count`、`table_count`、`image_count`、`parse_time`、`md_content`、`doc_metadata`。
- 新增 `sai_rag_document_image` 表，保存 RAG 文档解析出的图片、资源关联、页码、图注、章节、OCR 文本和 chunk 关联，并增加 rag/document/chunk/resource 维度索引。
- `sai_rag_chunk` 新增 `source_type` 字段和 `(rag_id, source_type)` 索引，用于区分 `TEXT` 文本 chunk 与 `IMAGE` 图片 chunk。
- `sai_mcp_server` 移除旧的 `version`、`auth_type`、`auth_config`、`capabilities` 字段，新增 `timeout` 与 `headers` 字段，对应 MCP 超时和请求头配置。
- PostgreSQL 与达梦脚本补充 `sai_rag_document_image.update_dt` 自动更新时间触发器。

### SQL 变更记录
- 对话记录表：`sai_agent_conversation_record` 增加 `metadata` 字段。
- RAG 文档表：`sai_rag_document` 增加 Docling 解析统计、Markdown 内容和元数据字段。
- RAG 图片表：新增 `sai_rag_document_image`，并增加 `rag_id`、`document_id`、`chunk_id`、`resource_id` 索引。
- RAG 分块表：`sai_rag_chunk` 增加 `source_type` 字段和 `(rag_id, source_type)` 索引。
- MCP 服务表：`sai_mcp_server` 删除 `version`、`auth_type`、`auth_config`、`capabilities`，新增 `timeout`、`headers`。

MySQL 升级参考 DDL：

```sql
ALTER TABLE sai_agent_conversation_record
    ADD COLUMN metadata LONGTEXT DEFAULT NULL COMMENT '消息扩展元数据JSON';

ALTER TABLE sai_rag_document
    ADD COLUMN page_count INT DEFAULT 0 COMMENT 'Docling parse page count',
    ADD COLUMN element_count INT DEFAULT 0 COMMENT 'Docling parse element count',
    ADD COLUMN table_count INT DEFAULT 0 COMMENT 'Docling parse table count',
    ADD COLUMN image_count INT DEFAULT 0 COMMENT 'Docling parse image count',
    ADD COLUMN parse_time INT DEFAULT 0 COMMENT 'Parse duration in milliseconds',
    ADD COLUMN md_content LONGTEXT DEFAULT NULL COMMENT 'Docling markdown content',
    ADD COLUMN doc_metadata LONGTEXT DEFAULT NULL COMMENT 'Docling document metadata JSON';

CREATE TABLE sai_rag_document_image
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rag_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_id BIGINT DEFAULT NULL,
    resource_id BIGINT DEFAULT NULL,
    image_index INT,
    image_url VARCHAR(1024),
    caption TEXT,
    figure_no VARCHAR(64),
    figure_title VARCHAR(512),
    section_title VARCHAR(512),
    source_page INT,
    document_name VARCHAR(255),
    ocr_text TEXT,
    create_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_rag_doc_image_rag ON sai_rag_document_image (rag_id);
CREATE INDEX idx_rag_doc_image_document ON sai_rag_document_image (document_id);
CREATE INDEX idx_rag_doc_image_chunk ON sai_rag_document_image (chunk_id);
CREATE INDEX idx_rag_doc_image_resource ON sai_rag_document_image (resource_id);

ALTER TABLE sai_rag_chunk
    ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'chunk来源类型：TEXT文本、IMAGE图片';

CREATE INDEX idx_chunk_rag_source_type ON sai_rag_chunk (rag_id, source_type);

ALTER TABLE sai_mcp_server
    DROP COLUMN version,
    DROP COLUMN auth_type,
    DROP COLUMN auth_config,
    DROP COLUMN capabilities,
    ADD COLUMN timeout BIGINT DEFAULT 60000 COMMENT '超时时间(毫秒)',
    ADD COLUMN headers TEXT COMMENT '请求头(JSON对象)';
```

### 文档与部署
- 新增 Docling 解析引擎设计文档、RAG 图片识别与证据匹配说明、图片对话设计方案和 Docker 部署说明。
- 更新管理端、OpenAPI、模型配置、RAG、Skill、独立聊天页等使用文档，并补充图片对话、模型视觉能力和 RAG 图片证据截图。
- Docker 部署增加 Docling 与 PaddleOCR 服务配置，完善环境变量模板和工具服务 README/API 文档。

### 升级说明
- 本版本涉及数据库结构变更，升级前请先备份数据库，并按实际数据库类型同步执行或合并新版 schema 中的字段、索引、触发器和新表变更。
- 已有 MCP 服务配置需要从旧的认证/能力字段迁移到 `timeout`、`headers` 配置模型，历史 `auth_type/auth_config/capabilities` 字段不再保留。
- 使用图片对话或 RAG 图片 OCR 前，需要在模型配置中为可处理图片的 CHAT 模型开启 `vision` 能力，并按需配置默认视觉模型。
- 使用 Docling/PaddleOCR 解析能力时，需要部署对应工具服务并配置 `.env`/Docker Compose 中的服务地址、超时和 OCR 参数。

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
