-- ============================================================
-- Snail AI 达梦数据库DM8建表脚本
-- 转换自：snail_ai_schema.sql (MySQL)
-- 达梦版本：DM8 V8 (DB Version: 0x7000d)
-- 说明：
--   1. AUTO_INCREMENT → IDENTITY(1,1)
--   2. LONGTEXT → CLOB
--   3. ENGINE/CHARSET/COLLATE → 移除达梦不适用
--   4. ON UPDATE CURRENT_TIMESTAMP → BEFORE UPDATE 触发器实现
--   5. COMMENT → COMMENT ON TABLE/COLUMN 语句
--   6. INSERT IGNORE → MERGE INTO 替代
--   7. KEY索引→ CREATE INDEX 语句
-- ============================================================


-- ============================================================
-- 一、用户与权限
-- ============================================================

-- 1.1 用户表
CREATE TABLE IF NOT EXISTS sai_user
(
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    role           INT,
    totals         INT,
    username       VARCHAR(255),
    nickname       VARCHAR(128)         DEFAULT NULL,
    email          VARCHAR(64),
    password       VARCHAR(255) NOT NULL,
    resource_id    BIGINT               DEFAULT NULL,
    create_dt      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_user_username UNIQUE (username)
);

COMMENT ON COLUMN sai_user.nickname IS '用户昵称';
COMMENT ON COLUMN sai_user.resource_id IS '头像资源ID，关联 sai_resource.id';

-- 1.2 OpenAPI 外部用户映射表
CREATE TABLE IF NOT EXISTS sai_openapi_user
(
    id               BIGINT       IDENTITY(1,1) PRIMARY KEY,
    app_id           VARCHAR(128) NOT NULL,
    open_id          VARCHAR(64)  NOT NULL,
    platform_user_id BIGINT       NOT NULL,
    external_id      VARCHAR(256) DEFAULT NULL,
    create_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_openapi_user_app_open UNIQUE (app_id, open_id),
    CONSTRAINT uk_sai_openapi_user_app_external UNIQUE (app_id, external_id)
);

COMMENT ON TABLE sai_openapi_user IS 'OpenAPI 外部用户映射表';
COMMENT ON COLUMN sai_openapi_user.app_id IS '关联 sai_app.app_id';
COMMENT ON COLUMN sai_openapi_user.open_id IS '平台分配的唯一标识UUID';
COMMENT ON COLUMN sai_openapi_user.platform_user_id IS '关联 sai_user.id，注册时自动创建';
COMMENT ON COLUMN sai_openapi_user.external_id IS '外部系统的用户标识可选，幂等用';

CREATE INDEX idx_sai_openapi_user_open_id ON sai_openapi_user (open_id);
CREATE INDEX idx_sai_openapi_user_platform_user ON sai_openapi_user (platform_user_id);


-- ============================================================
-- 二、AI 模型管理
-- ============================================================

-- 2.1 AI 模型提供商表
CREATE TABLE IF NOT EXISTS sai_model_provider
(
    id            BIGINT       IDENTITY(1,1) PRIMARY KEY,
    provider_name VARCHAR(255) NOT NULL,
    provider_key  VARCHAR(50)  NOT NULL,
    description   TEXT,
    icon_url      VARCHAR(500),
    is_enabled    TINYINT      DEFAULT 1,
    created_dt    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_dt    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_model_provider_name UNIQUE (provider_name),
    CONSTRAINT uk_sai_model_provider_key UNIQUE (provider_key)
);

COMMENT ON TABLE sai_model_provider IS 'AI模型提供商表';
COMMENT ON COLUMN sai_model_provider.provider_name IS '提供商名称';
COMMENT ON COLUMN sai_model_provider.provider_key IS '提供商标识符';
COMMENT ON COLUMN sai_model_provider.description IS '提供商描述';
COMMENT ON COLUMN sai_model_provider.icon_url IS 'LOGO图标URL';
COMMENT ON COLUMN sai_model_provider.is_enabled IS '是否启用';
COMMENT ON COLUMN sai_model_provider.created_dt IS '创建时间';
COMMENT ON COLUMN sai_model_provider.updated_dt IS '更新时间';

CREATE INDEX idx_sai_model_provider_key ON sai_model_provider (provider_key);
CREATE INDEX idx_sai_model_provider_is_enabled ON sai_model_provider (is_enabled);

-- 2.2 AI模型配置表
CREATE TABLE IF NOT EXISTS sai_model_config
(
    id           BIGINT       IDENTITY(1,1) PRIMARY KEY,
    provider_id  BIGINT       NOT NULL,
    model_name   VARCHAR(255) NOT NULL,
    model_key    VARCHAR(100) NOT NULL,
    model_type   VARCHAR(50)  NOT NULL,
    adapter_key  VARCHAR(100),
    description  VARCHAR(1000),
    api_key      VARCHAR(1000),
    api_endpoint VARCHAR(500),
    config_json  TEXT,
    owner_id     BIGINT,
    scope        VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL',
    is_default   TINYINT               DEFAULT 0,
    is_enabled   TINYINT               DEFAULT 1,
    created_dt   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_dt   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_model_config IS 'AI模型配置表';
COMMENT ON COLUMN sai_model_config.provider_id IS '提供商ID';
COMMENT ON COLUMN sai_model_config.model_name IS '模型名称';
COMMENT ON COLUMN sai_model_config.model_key IS '模型标识符';
COMMENT ON COLUMN sai_model_config.model_type IS '模型类型CHAT/EMBEDDING/RERANKER/IMAGE/SPEECH';
COMMENT ON COLUMN sai_model_config.adapter_key IS '底层协议适配器标识openai-compatible/http等';
COMMENT ON COLUMN sai_model_config.description IS '模型描述';
COMMENT ON COLUMN sai_model_config.api_key IS 'API密钥加密存储';
COMMENT ON COLUMN sai_model_config.api_endpoint IS 'API端点URL';
COMMENT ON COLUMN sai_model_config.config_json IS '模型参数配置JSON格式';
COMMENT ON COLUMN sai_model_config.owner_id IS '所有者IDNULL=全局,具体值=用户ID';
COMMENT ON COLUMN sai_model_config.scope IS '作用域GLOBAL/PERSONAL';
COMMENT ON COLUMN sai_model_config.is_default IS '是否为默认模型';
COMMENT ON COLUMN sai_model_config.is_enabled IS '是否启用';
COMMENT ON COLUMN sai_model_config.created_dt IS '创建时间';
COMMENT ON COLUMN sai_model_config.updated_dt IS '更新时间';

CREATE INDEX idx_sai_model_config_provider_model_type ON sai_model_config (provider_id, model_type);
CREATE INDEX idx_sai_model_config_model_type_enabled ON sai_model_config (model_type, is_enabled);
CREATE INDEX idx_sai_model_config_owner_id ON sai_model_config (owner_id);
CREATE INDEX idx_sai_model_config_is_default ON sai_model_config (is_default);
CREATE INDEX idx_sai_model_config_scope ON sai_model_config (scope);
CREATE INDEX idx_sai_model_config_model_key ON sai_model_config (model_key);
CREATE INDEX idx_sai_model_config_provider_id ON sai_model_config (provider_id);

-- 2.3 模型使用统计表
CREATE TABLE IF NOT EXISTS sai_model_usage_stat
(
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    model_id          BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    total_calls       BIGINT       DEFAULT 0,
    success_calls     BIGINT       DEFAULT 0,
    failed_calls      BIGINT       DEFAULT 0,
    total_tokens_used BIGINT       DEFAULT 0,
    total_cost        DECIMAL(18,8) DEFAULT 0,
    avg_response_time BIGINT       DEFAULT 0,
    last_used_dt      TIMESTAMP    DEFAULT NULL,
    created_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_model_usage_stat_model_user UNIQUE (model_id, user_id)
);

COMMENT ON TABLE sai_model_usage_stat IS '模型使用统计表';
COMMENT ON COLUMN sai_model_usage_stat.model_id IS '模型ID';
COMMENT ON COLUMN sai_model_usage_stat.user_id IS '用户ID';
COMMENT ON COLUMN sai_model_usage_stat.total_calls IS '总调用次数';
COMMENT ON COLUMN sai_model_usage_stat.success_calls IS '成功调用次数';
COMMENT ON COLUMN sai_model_usage_stat.failed_calls IS '失败调用次数';
COMMENT ON COLUMN sai_model_usage_stat.total_tokens_used IS '总Token使用量';
COMMENT ON COLUMN sai_model_usage_stat.total_cost IS '总费用';
COMMENT ON COLUMN sai_model_usage_stat.avg_response_time IS '平均响应时间毫秒';
COMMENT ON COLUMN sai_model_usage_stat.last_used_dt IS '最后使用时间';
COMMENT ON COLUMN sai_model_usage_stat.created_dt IS '创建时间';
COMMENT ON COLUMN sai_model_usage_stat.updated_dt IS '更新时间';

CREATE INDEX idx_sai_model_usage_stat_model_id ON sai_model_usage_stat (model_id);
CREATE INDEX idx_sai_model_usage_stat_user_id ON sai_model_usage_stat (user_id);
CREATE INDEX idx_sai_model_usage_stat_last_used_dt ON sai_model_usage_stat (last_used_dt);


-- ============================================================
-- 三、智能体Agent
-- ============================================================

-- 3.1 智能体主表
CREATE TABLE IF NOT EXISTS sai_agent
(
    id                      BIGINT       IDENTITY(1,1) PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    avatar                  VARCHAR(512),
    instruction             TEXT,
    greeting                TEXT,
    preset_questions        TEXT,
    chat_model_id           BIGINT,
    memory_enabled          TINYINT      DEFAULT 0,
    mcp_enabled             TINYINT      DEFAULT 0,
    skill_enabled           TINYINT      DEFAULT 0,
    web_search_enabled      TINYINT      DEFAULT 0,
    rag_enabled             TINYINT      DEFAULT 0,
    rag_ids                 VARCHAR(64)  DEFAULT NULL,
    rag_call_mode           TINYINT      DEFAULT 1,
    short_term_memory_size  INT          DEFAULT 20,
    creator_id              BIGINT,
    is_featured             TINYINT      DEFAULT 0,
    view_count              INT          DEFAULT 0,
    status                  TINYINT      DEFAULT 1,
    config                  TEXT,
    app_id                  VARCHAR(128) DEFAULT NULL,
    create_dt               TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt               TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_agent IS '智能体表';
COMMENT ON COLUMN sai_agent.name IS '智能体名称';
COMMENT ON COLUMN sai_agent.description IS '智能体描述';
COMMENT ON COLUMN sai_agent.avatar IS '头像URL';
COMMENT ON COLUMN sai_agent.instruction IS '系统指令System Prompt';
COMMENT ON COLUMN sai_agent.greeting IS '欢迎语';
COMMENT ON COLUMN sai_agent.preset_questions IS '预设问题列表JSON数组字符串';
COMMENT ON COLUMN sai_agent.chat_model_id IS '关联的对话模型ID';
COMMENT ON COLUMN sai_agent.memory_enabled IS '是否启用记忆库';
COMMENT ON COLUMN sai_agent.mcp_enabled IS '是否启用MCP';
COMMENT ON COLUMN sai_agent.skill_enabled IS '是否启用Skill';
COMMENT ON COLUMN sai_agent.web_search_enabled IS '是否启用联网搜索';
COMMENT ON COLUMN sai_agent.rag_enabled IS '是否启用RAG';
COMMENT ON COLUMN sai_agent.rag_ids IS '绑定的RAG ID列表，逗号分隔，最多5个';
COMMENT ON COLUMN sai_agent.rag_call_mode IS 'RAG调用方式: 1=智能调用 2=强制调用';
COMMENT ON COLUMN sai_agent.short_term_memory_size IS '短期记忆滑动窗口保留条数';
COMMENT ON COLUMN sai_agent.creator_id IS '创建者用户ID';
COMMENT ON COLUMN sai_agent.is_featured IS '是否精选';
COMMENT ON COLUMN sai_agent.view_count IS '浏览次数';
COMMENT ON COLUMN sai_agent.status IS '状态: 1-活跃 2-非活跃 3-已废弃 4-已禁用';
COMMENT ON COLUMN sai_agent.config IS '扩展配置预留';
COMMENT ON COLUMN sai_agent.app_id IS '关联应用IDNULL=本地执行';

CREATE INDEX idx_sai_agent_creator ON sai_agent (creator_id);
CREATE INDEX idx_sai_agent_featured ON sai_agent (is_featured);

-- 3.2 智能体对话表
CREATE TABLE IF NOT EXISTS sai_agent_conversation
(
    id              BIGINT      IDENTITY(1,1) PRIMARY KEY,
    agent_id        BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    title           VARCHAR(255),
    create_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_agent_conversation_conv_id UNIQUE (conversation_id)
);

COMMENT ON TABLE sai_agent_conversation IS '智能体对话表';
COMMENT ON COLUMN sai_agent_conversation.agent_id IS '智能体ID';
COMMENT ON COLUMN sai_agent_conversation.user_id IS '用户ID';
COMMENT ON COLUMN sai_agent_conversation.conversation_id IS '对话IDUUID';
COMMENT ON COLUMN sai_agent_conversation.title IS '对话标题';

CREATE INDEX idx_sai_agent_conversation_agent ON sai_agent_conversation (agent_id);
CREATE INDEX idx_sai_agent_conversation_user ON sai_agent_conversation (user_id);

-- 3.3 智能体对话消息记录表
CREATE TABLE IF NOT EXISTS sai_agent_conversation_record
(
    id              BIGINT      IDENTITY(1,1) PRIMARY KEY,
    agent_id        BIGINT      NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    user_id         BIGINT      NOT NULL,
    role            VARCHAR(16) DEFAULT 'user',
    content         TEXT,
    thinking        TEXT,
    status          INT         DEFAULT 1,
    input_tokens    INT         DEFAULT 0,
    output_tokens   INT         DEFAULT 0,
    cache_tokens    INT         DEFAULT 0,
    create_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_agent_conversation_record IS '智能体对话消息记录';
COMMENT ON COLUMN sai_agent_conversation_record.agent_id IS '智能体ID';
COMMENT ON COLUMN sai_agent_conversation_record.conversation_id IS '对话ID';
COMMENT ON COLUMN sai_agent_conversation_record.user_id IS '用户ID';
COMMENT ON COLUMN sai_agent_conversation_record.role IS 'user/assistant';
COMMENT ON COLUMN sai_agent_conversation_record.content IS '消息内容';
COMMENT ON COLUMN sai_agent_conversation_record.thinking IS '思考过程仅assistant';
COMMENT ON COLUMN sai_agent_conversation_record.status IS '1=成功,2=失败,3=进行中';
COMMENT ON COLUMN sai_agent_conversation_record.input_tokens IS '输入Token数prompt';
COMMENT ON COLUMN sai_agent_conversation_record.output_tokens IS '输出Token数completion';
COMMENT ON COLUMN sai_agent_conversation_record.cache_tokens IS '缓存命中Token数';

CREATE INDEX idx_sai_agent_conversation_record_conv ON sai_agent_conversation_record (conversation_id);

-- 3.4 智能体使用统计表
CREATE TABLE IF NOT EXISTS sai_agent_usage_stat
(
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    agent_id           BIGINT NOT NULL,
    user_id            BIGINT NOT NULL,
    message_count      INT    DEFAULT 0,
    conversation_count INT    DEFAULT 0,
    stat_date          DATE   NOT NULL,
    create_dt          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_dt          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_agent_usage_stat_agent_user_date UNIQUE (agent_id, user_id, stat_date)
);

COMMENT ON TABLE sai_agent_usage_stat IS '智能体使用统计';
COMMENT ON COLUMN sai_agent_usage_stat.agent_id IS '智能体ID';
COMMENT ON COLUMN sai_agent_usage_stat.user_id IS '用户ID';
COMMENT ON COLUMN sai_agent_usage_stat.message_count IS '消息条数';
COMMENT ON COLUMN sai_agent_usage_stat.conversation_count IS '对话轮次';
COMMENT ON COLUMN sai_agent_usage_stat.stat_date IS '统计日期';

CREATE INDEX idx_sai_agent_usage_stat_agent ON sai_agent_usage_stat (agent_id);
CREATE INDEX idx_sai_agent_usage_stat_date ON sai_agent_usage_stat (stat_date);

-- 3.5 用户订阅的智能体多对多
CREATE TABLE IF NOT EXISTS sai_user_agent
(
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    agent_id   BIGINT NOT NULL,
    create_dt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_user_agent_user_agent UNIQUE (user_id, agent_id)
);

COMMENT ON TABLE sai_user_agent IS '用户订阅的智能体';
COMMENT ON COLUMN sai_user_agent.user_id IS '用户ID';
COMMENT ON COLUMN sai_user_agent.agent_id IS '智能体ID';

CREATE INDEX idx_sai_user_agent_user ON sai_user_agent (user_id);


-- ============================================================
-- 四、RAG 知识库
-- ============================================================

-- 4.1 知识库主表
CREATE TABLE IF NOT EXISTS sai_rag
(
    id                        BIGINT       IDENTITY(1,1) PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL,
    description               TEXT,
    icon                      VARCHAR(512),
    embedding_model_id        BIGINT       NOT NULL,
    dimension_of_vector_model INT          NOT NULL,
    rerank_model_id           BIGINT,
    search_engine_instance_id BIGINT,
    vector_store_instance_id  BIGINT,
    search_engine_enable      TINYINT      DEFAULT 0,
    delimiter                 VARCHAR(32)  DEFAULT '\n\n',
    rag_enhancement           TEXT,
    config                    TEXT         DEFAULT NULL,
    dedup_strategy            TINYINT      NOT NULL DEFAULT 2,
    dedup_action              TINYINT      NOT NULL DEFAULT 0,
    upload_confirm            TINYINT      NOT NULL DEFAULT 1,
    create_dt                 TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt                 TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_rag IS 'RAG知识库主表';
COMMENT ON COLUMN sai_rag.name IS '知识库名称';
COMMENT ON COLUMN sai_rag.description IS '知识库描述';
COMMENT ON COLUMN sai_rag.icon IS '图标URL';
COMMENT ON COLUMN sai_rag.embedding_model_id IS 'Embedding模型ID';
COMMENT ON COLUMN sai_rag.dimension_of_vector_model IS '向量维度';
COMMENT ON COLUMN sai_rag.rerank_model_id IS 'Rerank模型ID';
COMMENT ON COLUMN sai_rag.search_engine_instance_id IS '搜索引擎实例ID';
COMMENT ON COLUMN sai_rag.vector_store_instance_id IS '向量存储实例ID';
COMMENT ON COLUMN sai_rag.search_engine_enable IS '是否启用搜索引擎';
COMMENT ON COLUMN sai_rag.delimiter IS '文档分隔符';
COMMENT ON COLUMN sai_rag.rag_enhancement IS 'RAG增强配置';
COMMENT ON COLUMN sai_rag.config IS 'RAG检索和问答的页面配置参数';
COMMENT ON COLUMN sai_rag.dedup_strategy IS '去重策略: 0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT';
COMMENT ON COLUMN sai_rag.dedup_action IS '冲突动作: 0=REJECT 1=SKIP 2=OVERWRITE';
COMMENT ON COLUMN sai_rag.upload_confirm IS '上传前二次确认: 0-关 1-开';

-- 4.2 RAG 文档表
CREATE TABLE IF NOT EXISTS sai_rag_document
(
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    rag_id       BIGINT NOT NULL,
    name         VARCHAR(255),
    file_type    VARCHAR(32),
    source_type  VARCHAR(32),
    source_path  VARCHAR(1024),
    storage_path VARCHAR(1024),
    storage_type VARCHAR(32) DEFAULT 'LOCAL',
    file_size    BIGINT      DEFAULT 0,
    content      TEXT,
    status       TINYINT     DEFAULT 0,
    error_msg    TEXT,
    chunk_count  INT         DEFAULT 0,
    content_hash VARCHAR(64) DEFAULT NULL,
    resource_id  BIGINT      DEFAULT NULL,
    create_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_rag_document IS 'RAG文档表';
COMMENT ON COLUMN sai_rag_document.status IS '状态: 0-待处理 1-解析中 2-处理中 3-处理完成 4-处理失败';
COMMENT ON COLUMN sai_rag_document.content_hash IS '文件内容SHA-256哈希，用于去重';
COMMENT ON COLUMN sai_rag_document.resource_id IS '关联资源库 sai_resource.id';

CREATE INDEX idx_sai_rag_document_rag ON sai_rag_document (rag_id);
CREATE INDEX idx_sai_rag_document_content_hash ON sai_rag_document (rag_id, content_hash);
CREATE INDEX idx_sai_rag_document_name ON sai_rag_document (rag_id, name);
CREATE INDEX idx_sai_rag_document_resource ON sai_rag_document (resource_id);

-- 4.3 RAG 分块表
CREATE TABLE IF NOT EXISTS sai_rag_chunk
(
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    rag_id          BIGINT NOT NULL,
    document_id     BIGINT NOT NULL,
    paragraph_index INT,
    chunk_index     INT,
    content         TEXT,
    token_count     INT,
    vector_id       VARCHAR(128),
    content_hash    VARCHAR(64) DEFAULT NULL,
    create_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_rag_chunk IS 'RAG分块表';
COMMENT ON COLUMN sai_rag_chunk.content_hash IS 'chunk内容SHA-256，用于向量去重';

CREATE INDEX idx_sai_rag_chunk_rag ON sai_rag_chunk (rag_id);
CREATE INDEX idx_sai_rag_chunk_document ON sai_rag_chunk (document_id);
CREATE INDEX idx_sai_rag_chunk_hash ON sai_rag_chunk (rag_id, content_hash);


-- ============================================================
-- 五、MCP 服务管理
-- ============================================================

-- 5.1 MCP 服务配置表
CREATE TABLE IF NOT EXISTS sai_mcp_server
(
    id               BIGINT        IDENTITY(1,1) PRIMARY KEY,
    name             VARCHAR(255)  NOT NULL,
    description      TEXT,
    transport_type   TINYINT       DEFAULT 1,
    base_uri         VARCHAR(1024),
    endpoint         VARCHAR(1024),
    command          VARCHAR(1024),
    args             TEXT,
    env_vars         TEXT,
    version          VARCHAR(32)   DEFAULT '1.0.0',
    auth_type        TINYINT       DEFAULT 0,
    auth_config      TEXT,
    capabilities     TEXT,
    last_connect_dt  TIMESTAMP     DEFAULT NULL,
    creator_id       BIGINT,
    create_dt        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_mcp_server IS 'MCP服务配置表';
COMMENT ON COLUMN sai_mcp_server.name IS 'MCP服务名称';
COMMENT ON COLUMN sai_mcp_server.description IS 'MCP服务描述';
COMMENT ON COLUMN sai_mcp_server.transport_type IS '传输类型: 1-SSE 2-Streamable HTTP 3-Stdio';
COMMENT ON COLUMN sai_mcp_server.base_uri IS '服务基础地址SSE/Streamable HTTP时使用';
COMMENT ON COLUMN sai_mcp_server.endpoint IS '端点路径SSE/Streamable HTTP时可选';
COMMENT ON COLUMN sai_mcp_server.command IS 'Stdio命令Stdio时必填';
COMMENT ON COLUMN sai_mcp_server.args IS 'Stdio命令参数JSON数组';
COMMENT ON COLUMN sai_mcp_server.env_vars IS 'Stdio环境变量JSON对象';
COMMENT ON COLUMN sai_mcp_server.version IS '版本';
COMMENT ON COLUMN sai_mcp_server.auth_type IS '认证方式: 0-无需认证 1-API Key 2-OAuth 3-Basic Auth';
COMMENT ON COLUMN sai_mcp_server.auth_config IS '认证配置JSON';
COMMENT ON COLUMN sai_mcp_server.capabilities IS '能力列表JSON数组';
COMMENT ON COLUMN sai_mcp_server.last_connect_dt IS '最后连接时间';
COMMENT ON COLUMN sai_mcp_server.creator_id IS '创建者用户ID';

CREATE INDEX idx_sai_mcp_server_creator ON sai_mcp_server (creator_id);

-- 5.2 智能体与MCP服务关联表多对多
CREATE TABLE IF NOT EXISTS sai_agent_mcp_server
(
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    agent_id      BIGINT NOT NULL,
    mcp_server_id BIGINT NOT NULL,
    create_dt     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_agent_mcp_server UNIQUE (agent_id, mcp_server_id)
);

COMMENT ON TABLE sai_agent_mcp_server IS '智能体MCP服务关联表';
COMMENT ON COLUMN sai_agent_mcp_server.agent_id IS '智能体ID';
COMMENT ON COLUMN sai_agent_mcp_server.mcp_server_id IS 'MCP服务ID';

CREATE INDEX idx_sai_agent_mcp_server_agent ON sai_agent_mcp_server (agent_id);
CREATE INDEX idx_sai_agent_mcp_server_server ON sai_agent_mcp_server (mcp_server_id);


-- ============================================================
-- 六、Skill 技能包管理
-- ============================================================

-- 6.1 Skill 技能包表
CREATE TABLE IF NOT EXISTS sai_skill
(
    id            BIGINT        IDENTITY(1,1) PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    description   TEXT,
    file_name     VARCHAR(255),
    file_path     VARCHAR(1024),
    file_size     BIGINT        DEFAULT 0,
    skill_content CLOB,
    storage_path  VARCHAR(500)  DEFAULT NULL,
    version       BIGINT        DEFAULT 0,
    has_files     TINYINT       DEFAULT 0,
    creator_id    BIGINT,
    create_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_skill IS 'Skill技能包表';
COMMENT ON COLUMN sai_skill.name IS 'Skill名称从SKILL.md解析';
COMMENT ON COLUMN sai_skill.description IS 'Skill描述从SKILL.md解析';
COMMENT ON COLUMN sai_skill.file_name IS '上传的zip文件名';
COMMENT ON COLUMN sai_skill.file_path IS '解压后存储路径';
COMMENT ON COLUMN sai_skill.file_size IS '文件大小字节';
COMMENT ON COLUMN sai_skill.skill_content IS 'SKILL.md正文内容去除frontmatter';
COMMENT ON COLUMN sai_skill.storage_path IS '对象存储相对路径前缀如 skills/123/';
COMMENT ON COLUMN sai_skill.version IS '版本号，文件变更时自增，用于缓存一致性校验';
COMMENT ON COLUMN sai_skill.has_files IS '是否包含支撑文件0=仅SKILL.md，1=有scripts/references等';
COMMENT ON COLUMN sai_skill.creator_id IS '创建者用户ID';

CREATE INDEX idx_sai_skill_creator ON sai_skill (creator_id);

-- 6.2 Skill 支撑文件内容表
CREATE TABLE IF NOT EXISTS sai_skill_file
(
    id         BIGINT       IDENTITY(1,1) PRIMARY KEY,
    skill_id   BIGINT       NOT NULL,
    file_path  VARCHAR(255) NOT NULL,
    content    CLOB         NOT NULL,
    file_size  INT          NOT NULL,
    encoding   VARCHAR(50)  DEFAULT 'utf-8',
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_skill_file_path UNIQUE (skill_id, file_path)
);

COMMENT ON TABLE sai_skill_file IS 'Skill支撑文件内容表';
COMMENT ON COLUMN sai_skill_file.skill_id IS 'Skill ID';
COMMENT ON COLUMN sai_skill_file.file_path IS '文件相对路径';
COMMENT ON COLUMN sai_skill_file.content IS '文件内容';
COMMENT ON COLUMN sai_skill_file.file_size IS '文件大小字节';
COMMENT ON COLUMN sai_skill_file.encoding IS '编码方式';
COMMENT ON COLUMN sai_skill_file.created_at IS '创建时间';
COMMENT ON COLUMN sai_skill_file.updated_at IS '更新时间';

CREATE INDEX idx_sai_skill_file_skill_id ON sai_skill_file (skill_id);

-- 6.3 智能体与Skill关联表多对多
CREATE TABLE IF NOT EXISTS sai_agent_skill
(
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    agent_id   BIGINT NOT NULL,
    skill_id   BIGINT NOT NULL,
    create_dt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_agent_skill UNIQUE (agent_id, skill_id)
);

COMMENT ON TABLE sai_agent_skill IS '智能体Skill关联表';
COMMENT ON COLUMN sai_agent_skill.agent_id IS '智能体ID';
COMMENT ON COLUMN sai_agent_skill.skill_id IS 'Skill ID';

CREATE INDEX idx_sai_agent_skill_agent ON sai_agent_skill (agent_id);
CREATE INDEX idx_sai_agent_skill_skill ON sai_agent_skill (skill_id);


-- ============================================================
-- 七、客户端应用与节点
-- ============================================================

-- 7.1 客户端应用
CREATE TABLE IF NOT EXISTS sai_app
(
    id             BIGINT       IDENTITY(1,1) PRIMARY KEY,
    app_id         VARCHAR(128) NOT NULL,
    app_name       VARCHAR(255) NOT NULL,
    description    VARCHAR(512),
    token          VARCHAR(128) NOT NULL,
    route_strategy VARCHAR(32)  DEFAULT 'LEAST_LOAD',
    status         TINYINT      DEFAULT 1,
    create_dt      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_app_app_id UNIQUE (app_id)
);

COMMENT ON TABLE sai_app IS '客户端应用';
COMMENT ON COLUMN sai_app.app_id IS '应用唯一标识';
COMMENT ON COLUMN sai_app.app_name IS '应用名称';
COMMENT ON COLUMN sai_app.description IS '应用描述';
COMMENT ON COLUMN sai_app.token IS '通信认证令牌';
COMMENT ON COLUMN sai_app.route_strategy IS '路由策略';
COMMENT ON COLUMN sai_app.status IS '1=启用, 0=停用';

-- 7.2 AI客户端实例节点
CREATE TABLE IF NOT EXISTS sai_client_node
(
    id                  BIGINT       IDENTITY(1,1) PRIMARY KEY,
    app_id              VARCHAR(128) NOT NULL,
    host_id             VARCHAR(128) NOT NULL,
    host_ip             VARCHAR(64)  NOT NULL,
    grpc_port           INT          NOT NULL,
    max_concurrent      INT          DEFAULT 10,
    active_chats        INT          DEFAULT 0,
    supported_providers TEXT,
    labels              TEXT,
    expire_dt           TIMESTAMP    NOT NULL,
    create_dt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_client_node UNIQUE (app_id, host_id)
);

COMMENT ON TABLE sai_client_node IS 'AI客户端实例节点';
COMMENT ON COLUMN sai_client_node.app_id IS '所属应用ID';
COMMENT ON COLUMN sai_client_node.host_id IS '客户端实例唯一标识';
COMMENT ON COLUMN sai_client_node.host_ip IS '客户端IP';
COMMENT ON COLUMN sai_client_node.grpc_port IS '客户端gRPC端口';
COMMENT ON COLUMN sai_client_node.max_concurrent IS '最大并发对话数';
COMMENT ON COLUMN sai_client_node.active_chats IS '当前活跃对话数';
COMMENT ON COLUMN sai_client_node.supported_providers IS '支持的模型提供商JSON数组';
COMMENT ON COLUMN sai_client_node.labels IS '路由标签';
COMMENT ON COLUMN sai_client_node.expire_dt IS '过期时间心跳更新';

CREATE INDEX idx_sai_client_node_app_expire ON sai_client_node (app_id, expire_dt);


-- ============================================================
-- 八、存储与资源
-- ============================================================

-- 8.1 存储实例
CREATE TABLE IF NOT EXISTS sai_store_instance
(
    id         BIGINT       IDENTITY(1,1) PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    category   TINYINT      NOT NULL,
    type       TINYINT      NOT NULL,
    config     TEXT         DEFAULT NULL,
    status     TINYINT      DEFAULT 1,
    is_default TINYINT      DEFAULT 0,
    create_dt  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sai_store_instance IS '存储实例';
COMMENT ON COLUMN sai_store_instance.name IS '实例名称';
COMMENT ON COLUMN sai_store_instance.category IS '分类: 1-向量库 2-搜索引擎';
COMMENT ON COLUMN sai_store_instance.type IS '类型: 1-PG_VECTOR 2-MILVUS 3-ELASTICSEARCH 4-PG_FULLTEXT';
COMMENT ON COLUMN sai_store_instance.config IS '连接参数 JSON';
COMMENT ON COLUMN sai_store_instance.status IS '状态: 0-停用 1-启用';
COMMENT ON COLUMN sai_store_instance.is_default IS '是否为该 category 下默认实例';

CREATE INDEX idx_sai_store_instance_category ON sai_store_instance (category);
CREATE INDEX idx_sai_store_instance_type ON sai_store_instance (type);

-- 8.2 通用资源存储
CREATE TABLE IF NOT EXISTS sai_resource
(
    id            BIGINT        IDENTITY(1,1) PRIMARY KEY,
    storage_key   VARCHAR(512)  NOT NULL,
    original_name VARCHAR(255)  NOT NULL,
    file_size     BIGINT        DEFAULT 0,
    mime_type     VARCHAR(128),
    storage_type  VARCHAR(32)   NOT NULL DEFAULT 'LOCAL',
    access_url    VARCHAR(1024),
    biz_type      VARCHAR(64)   NOT NULL DEFAULT 'GENERAL',
    biz_id        BIGINT,
    creator_id    BIGINT,
    create_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sai_resource_storage_key UNIQUE (storage_key)
);

COMMENT ON TABLE sai_resource IS '通用资源存储';
COMMENT ON COLUMN sai_resource.storage_key IS '存储键相对路径或对象Key';
COMMENT ON COLUMN sai_resource.original_name IS '原始文件名';
COMMENT ON COLUMN sai_resource.file_size IS '文件大小bytes';
COMMENT ON COLUMN sai_resource.mime_type IS 'MIME类型';
COMMENT ON COLUMN sai_resource.storage_type IS '存储类型: LOCAL/MINIO';
COMMENT ON COLUMN sai_resource.access_url IS '访问URL';
COMMENT ON COLUMN sai_resource.biz_type IS '业务类型: AVATAR/ATTACHMENT/DOCUMENT/GENERAL';
COMMENT ON COLUMN sai_resource.biz_id IS '关联业务ID';
COMMENT ON COLUMN sai_resource.creator_id IS '上传者ID';

CREATE INDEX idx_sai_resource_biz ON sai_resource (biz_type, biz_id);
CREATE INDEX idx_sai_resource_creator ON sai_resource (creator_id);


-- ============================================================
-- 九、触发器替代 MySQL 的 ON UPDATE CURRENT_TIMESTAMP
-- ============================================================

-- sai_user.update_dt
CREATE OR REPLACE TRIGGER trg_sai_user_update_dt
    BEFORE UPDATE ON sai_user
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_openapi_user.update_dt
CREATE OR REPLACE TRIGGER trg_sai_openapi_user_update_dt
    BEFORE UPDATE ON sai_openapi_user
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_model_provider.updated_dt
CREATE OR REPLACE TRIGGER trg_sai_model_provider_updated_dt
    BEFORE UPDATE ON sai_model_provider
    FOR EACH ROW
BEGIN
    :NEW.updated_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_model_config.updated_dt
CREATE OR REPLACE TRIGGER trg_sai_model_config_updated_dt
    BEFORE UPDATE ON sai_model_config
    FOR EACH ROW
BEGIN
    :NEW.updated_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_model_usage_stat.updated_dt
CREATE OR REPLACE TRIGGER trg_sai_model_usage_stat_updated_dt
    BEFORE UPDATE ON sai_model_usage_stat
    FOR EACH ROW
BEGIN
    :NEW.updated_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_agent.update_dt
CREATE OR REPLACE TRIGGER trg_sai_agent_update_dt
    BEFORE UPDATE ON sai_agent
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_agent_conversation.update_dt
CREATE OR REPLACE TRIGGER trg_sai_agent_conversation_update_dt
    BEFORE UPDATE ON sai_agent_conversation
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_agent_usage_stat.update_dt
CREATE OR REPLACE TRIGGER trg_sai_agent_usage_stat_update_dt
    BEFORE UPDATE ON sai_agent_usage_stat
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_rag.update_dt
CREATE OR REPLACE TRIGGER trg_sai_rag_update_dt
    BEFORE UPDATE ON sai_rag
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_rag_document.update_dt
CREATE OR REPLACE TRIGGER trg_sai_rag_document_update_dt
    BEFORE UPDATE ON sai_rag_document
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_rag_chunk.update_dt
CREATE OR REPLACE TRIGGER trg_sai_rag_chunk_update_dt
    BEFORE UPDATE ON sai_rag_chunk
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_mcp_server.update_dt
CREATE OR REPLACE TRIGGER trg_sai_mcp_server_update_dt
    BEFORE UPDATE ON sai_mcp_server
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_skill.update_dt
CREATE OR REPLACE TRIGGER trg_sai_skill_update_dt
    BEFORE UPDATE ON sai_skill
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_skill_file.updated_at
CREATE OR REPLACE TRIGGER trg_sai_skill_file_updated_at
    BEFORE UPDATE ON sai_skill_file
    FOR EACH ROW
BEGIN
    :NEW.updated_at = CURRENT_TIMESTAMP;
END;
/

-- sai_app.update_dt
CREATE OR REPLACE TRIGGER trg_sai_app_update_dt
    BEFORE UPDATE ON sai_app
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_client_node.update_dt
CREATE OR REPLACE TRIGGER trg_sai_client_node_update_dt
    BEFORE UPDATE ON sai_client_node
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_store_instance.update_dt
CREATE OR REPLACE TRIGGER trg_sai_store_instance_update_dt
    BEFORE UPDATE ON sai_store_instance
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/

-- sai_resource.update_dt
CREATE OR REPLACE TRIGGER trg_sai_resource_update_dt
    BEFORE UPDATE ON sai_resource
    FOR EACH ROW
BEGIN
    :NEW.update_dt = CURRENT_TIMESTAMP;
END;
/


-- ============================================================
-- 十、初始化数据
-- ============================================================

-- 10.1 默认管理员：admin / admin123
SET IDENTITY_INSERT sai_user ON;
INSERT INTO sai_user (id, role, username, email, password, create_dt, update_dt)
VALUES (1, 2, 'admin', '', 'pbkdf2$120000$c25haWwtYWktYWRtaW4tMQ==$kakglT/wYKOgv/77Ah1stie58d/JbY2nGgq5DwgUBw4=',
        TIMESTAMP '2026-02-11 13:56:48.210429', TIMESTAMP '2026-02-11 13:56:48.210429');
SET IDENTITY_INSERT sai_user OFF;

-- 10.2 插入常见的AI提供商使用 MERGE INTO 模拟 INSERT IGNORE
-- 10.2 插入常见的AI提供商
INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT 'OpenAI', 'openai', 'OpenAI官方模型 GPT-4 GPT-3.5等', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = 'OpenAI');

INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT 'Claude', 'claude', 'Anthropic Claude模型', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = 'Claude');

INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT 'Ollama', 'ollama', '本地开源模型 Llama Mistral等', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = 'Ollama');

INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT 'Google Gemini', 'gemini', 'Google Gemini模型', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = 'Google Gemini');

INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT '阿里云百炼', 'qwen', '阿里云百炼 OpenAI 兼容模型 Qwen等', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = '阿里云百炼');

INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT 'DeepSeek', 'deepseek', 'DeepSeek OpenAI 兼容模型', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = 'DeepSeek');

INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
SELECT '智谱AI', 'zhipu', '智谱AI OpenAI 兼容模型 GLM等', 1
WHERE NOT EXISTS (SELECT 1 FROM sai_model_provider WHERE provider_name = '智谱AI');
-- 10.3 初始化测试应用
SET IDENTITY_INSERT sai_app ON;
INSERT INTO sai_app (id, app_id, app_name, description, token, route_strategy, status, create_dt, update_dt)
VALUES (1, 'snail-ai-agent-demo', 'snail-ai-agent-demo', '', 'SAI_3ce13fa4e56a43c2b42e380c649629a5', 'LEAST_LOAD', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SET IDENTITY_INSERT sai_app OFF;

-- 10.4 初始化测试的openid
SET IDENTITY_INSERT sai_openapi_user ON;
INSERT INTO sai_openapi_user (id, app_id, open_id, platform_user_id, external_id, create_dt, update_dt)
VALUES (1, 'snail-ai-agent-demo', '46ed53c6a20044c7bbd870848e80f92f', 1, '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SET IDENTITY_INSERT sai_openapi_user OFF;

COMMIT;

-- ============================================================
-- 脚本结束
-- 使用方式：在达梦管理工具DM Manager/DISQL中执行此脚本即可
-- disql SYSDBA/SYSDBA@localhost:5236 -f snail_ai_schema_dameng.sql
-- ============================================================
