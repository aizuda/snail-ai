-- ============================================================
-- Snail AI PostgreSQL 全量建表脚本（仅 CREATE，无 ALTER）
-- 使用：psql -U user -d database -f snail_ai_schema_postgresql.sql
-- ============================================================

-- MySQL 的 ON UPDATE CURRENT_TIMESTAMP 在 PostgreSQL 中用触发器模拟。
CREATE OR REPLACE FUNCTION sai_touch_update_dt()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_dt = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sai_touch_updated_dt()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_dt = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sai_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Enterprise RAG Schema for PostgreSQL
-- ============================================================

-- Knowledge Base
CREATE TABLE IF NOT EXISTS sai_rag
(
    id                        BIGSERIAL PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL,
    description               TEXT,
    icon                      VARCHAR(512),
    embedding_model_id        BIGINT       NOT NULL,
    dimension_of_vector_model INTEGER      NOT NULL,
    rerank_model_id           BIGINT,
    search_engine_instance_id BIGINT,
    vector_store_instance_id  BIGINT,
    search_engine_enable      BOOLEAN      DEFAULT FALSE,
    delimiter                 VARCHAR(32)  DEFAULT E'\n\n',
    rag_enhancement           TEXT,
    config                    TEXT         DEFAULT NULL,
    dedup_strategy            INTEGER      NOT NULL DEFAULT 2,
    dedup_action              INTEGER      NOT NULL DEFAULT 0,
    upload_confirm            BOOLEAN      NOT NULL DEFAULT TRUE,
    create_dt                 TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt                 TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_rag_touch_update_dt ON sai_rag;
CREATE TRIGGER trg_sai_rag_touch_update_dt
    BEFORE UPDATE ON sai_rag
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

-- RAG Documents
CREATE TABLE IF NOT EXISTS sai_rag_document
(
    id           BIGSERIAL PRIMARY KEY,
    rag_id       BIGINT NOT NULL,
    name         VARCHAR(255),
    file_type    VARCHAR(32),
    source_type  VARCHAR(32),
    source_path  VARCHAR(1024),
    storage_path VARCHAR(1024),
    storage_type VARCHAR(32) DEFAULT 'LOCAL',
    file_size    BIGINT      DEFAULT 0,
    content      TEXT,
    status       INTEGER     DEFAULT 0,
    error_msg    TEXT,
    chunk_count  INTEGER     DEFAULT 0,
    content_hash VARCHAR(64) DEFAULT NULL,
    resource_id  BIGINT      DEFAULT NULL,
    create_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_rag_document_touch_update_dt ON sai_rag_document;
CREATE TRIGGER trg_sai_rag_document_touch_update_dt
    BEFORE UPDATE ON sai_rag_document
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_rag_doc_rag ON sai_rag_document (rag_id);
CREATE INDEX IF NOT EXISTS idx_rag_content_hash ON sai_rag_document (rag_id, content_hash);
CREATE INDEX IF NOT EXISTS idx_rag_name ON sai_rag_document (rag_id, name);
CREATE INDEX IF NOT EXISTS idx_rag_doc_resource ON sai_rag_document (resource_id);

-- RAG Chunks
CREATE TABLE IF NOT EXISTS sai_rag_chunk
(
    id              BIGSERIAL PRIMARY KEY,
    rag_id          BIGINT NOT NULL,
    document_id     BIGINT NOT NULL,
    paragraph_index INTEGER,
    chunk_index     INTEGER,
    content         TEXT,
    token_count     INTEGER,
    vector_id       VARCHAR(128),
    content_hash    VARCHAR(64) DEFAULT NULL,
    create_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_rag_chunk_touch_update_dt ON sai_rag_chunk;
CREATE TRIGGER trg_sai_rag_chunk_touch_update_dt
    BEFORE UPDATE ON sai_rag_chunk
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_rag_chunk_rag ON sai_rag_chunk (rag_id);
CREATE INDEX IF NOT EXISTS idx_rag_chunk_document ON sai_rag_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_rag_hash ON sai_rag_chunk (rag_id, content_hash);

CREATE TABLE IF NOT EXISTS sai_user
(
    id        BIGSERIAL PRIMARY KEY,
    role      INTEGER,
    totals    INTEGER,
    username  VARCHAR(255),
    email     VARCHAR(64),
    password  VARCHAR(255) NOT NULL,
    create_dt TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_username UNIQUE (username)
);

DROP TRIGGER IF EXISTS trg_sai_user_touch_update_dt ON sai_user;
CREATE TRIGGER trg_sai_user_touch_update_dt
    BEFORE UPDATE ON sai_user
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

-- ============================================
-- 1. AI 模型提供商表
-- ============================================
CREATE TABLE IF NOT EXISTS sai_model_provider
(
    id            BIGSERIAL PRIMARY KEY,
    provider_name VARCHAR(255) NOT NULL,
    provider_key  VARCHAR(50)  NOT NULL,
    description   TEXT,
    icon_url      VARCHAR(500),
    is_enabled    BOOLEAN     DEFAULT TRUE,
    created_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_name UNIQUE (provider_name),
    CONSTRAINT uk_provider_key UNIQUE (provider_key)
);

DROP TRIGGER IF EXISTS trg_sai_model_provider_touch_updated_dt ON sai_model_provider;
CREATE TRIGGER trg_sai_model_provider_touch_updated_dt
    BEFORE UPDATE ON sai_model_provider
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_updated_dt();

CREATE INDEX IF NOT EXISTS idx_provider_key ON sai_model_provider (provider_key);
CREATE INDEX IF NOT EXISTS idx_is_enabled ON sai_model_provider (is_enabled);

-- ============================================
-- 2. AI模型配置表
-- ============================================
CREATE TABLE IF NOT EXISTS sai_model_config
(
    id           BIGSERIAL PRIMARY KEY,
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
    is_default   BOOLEAN      DEFAULT FALSE,
    is_enabled   BOOLEAN      DEFAULT TRUE,
    created_dt   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_dt   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_model_config_touch_updated_dt ON sai_model_config;
CREATE TRIGGER trg_sai_model_config_touch_updated_dt
    BEFORE UPDATE ON sai_model_config
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_updated_dt();

CREATE INDEX IF NOT EXISTS fk_provider_id ON sai_model_config (provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_model_type ON sai_model_config (provider_id, model_type);
CREATE INDEX IF NOT EXISTS idx_model_type_enabled ON sai_model_config (model_type, is_enabled);
CREATE INDEX IF NOT EXISTS idx_owner_id ON sai_model_config (owner_id);
CREATE INDEX IF NOT EXISTS idx_is_default ON sai_model_config (is_default);
CREATE INDEX IF NOT EXISTS idx_scope ON sai_model_config (scope);
CREATE INDEX IF NOT EXISTS idx_model_key ON sai_model_config (model_key);

-- ============================================
-- 3. 模型使用统计表
-- ============================================
CREATE TABLE IF NOT EXISTS sai_model_usage_stat
(
    id                BIGSERIAL PRIMARY KEY,
    model_id          BIGINT         NOT NULL,
    user_id           BIGINT         NOT NULL,
    total_calls       BIGINT         DEFAULT 0,
    success_calls     BIGINT         DEFAULT 0,
    failed_calls      BIGINT         DEFAULT 0,
    total_tokens_used BIGINT         DEFAULT 0,
    total_cost        DECIMAL(18, 8) DEFAULT 0,
    avg_response_time BIGINT         DEFAULT 0,
    last_used_dt      TIMESTAMP NULL DEFAULT NULL,
    created_dt        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_dt        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_model_user UNIQUE (model_id, user_id)
);

DROP TRIGGER IF EXISTS trg_sai_model_usage_stat_touch_updated_dt ON sai_model_usage_stat;
CREATE TRIGGER trg_sai_model_usage_stat_touch_updated_dt
    BEFORE UPDATE ON sai_model_usage_stat
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_updated_dt();

CREATE INDEX IF NOT EXISTS fk_stat_model_id ON sai_model_usage_stat (model_id);
CREATE INDEX IF NOT EXISTS idx_model_id ON sai_model_usage_stat (model_id);
CREATE INDEX IF NOT EXISTS idx_user_id ON sai_model_usage_stat (user_id);
CREATE INDEX IF NOT EXISTS idx_last_used_dt ON sai_model_usage_stat (last_used_dt);

-- ============================================
-- 智能体相关表
-- ============================================

-- 智能体主表
CREATE TABLE IF NOT EXISTS sai_agent
(
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    avatar                 VARCHAR(512),
    instruction            TEXT,
    greeting               TEXT,
    preset_questions       TEXT,
    chat_model_id          BIGINT,
    memory_enabled         BOOLEAN     DEFAULT FALSE,
    mcp_enabled            BOOLEAN     DEFAULT FALSE,
    skill_enabled          BOOLEAN     DEFAULT FALSE,
    web_search_enabled     BOOLEAN     DEFAULT FALSE,
    rag_enabled            BOOLEAN     DEFAULT FALSE,
    rag_ids                VARCHAR(64) NULL,
    rag_call_mode          INTEGER     DEFAULT 1,
    short_term_memory_size INTEGER     DEFAULT 20,
    creator_id             BIGINT,
    is_featured            BOOLEAN     DEFAULT FALSE,
    view_count             INTEGER     DEFAULT 0,
    status                 INTEGER     DEFAULT 1,
    config                 TEXT,
    app_id                 VARCHAR(128) NULL,
    create_dt              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_agent_touch_update_dt ON sai_agent;
CREATE TRIGGER trg_sai_agent_touch_update_dt
    BEFORE UPDATE ON sai_agent
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_agent_creator ON sai_agent (creator_id);
CREATE INDEX IF NOT EXISTS idx_agent_featured ON sai_agent (is_featured);

-- 智能体对话表
CREATE TABLE IF NOT EXISTS sai_agent_conversation
(
    id              BIGSERIAL PRIMARY KEY,
    agent_id        BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    title           VARCHAR(255),
    create_dt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conv_id UNIQUE (conversation_id)
);

DROP TRIGGER IF EXISTS trg_sai_agent_conversation_touch_update_dt ON sai_agent_conversation;
CREATE TRIGGER trg_sai_agent_conversation_touch_update_dt
    BEFORE UPDATE ON sai_agent_conversation
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_agent_conv_agent ON sai_agent_conversation (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_conv_user ON sai_agent_conversation (user_id);

-- 智能体对话消息记录表
CREATE TABLE IF NOT EXISTS sai_agent_conversation_record
(
    id              BIGSERIAL PRIMARY KEY,
    agent_id        BIGINT      NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    user_id         BIGINT      NOT NULL,
    role            VARCHAR(16) DEFAULT 'user',
    content         TEXT,
    thinking        TEXT,
    status          INTEGER     DEFAULT 1,
    input_tokens    INTEGER     DEFAULT 0,
    output_tokens   INTEGER     DEFAULT 0,
    cache_tokens    INTEGER     DEFAULT 0,
    create_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_rec_conv ON sai_agent_conversation_record (conversation_id);

-- 智能体使用统计表
CREATE TABLE IF NOT EXISTS sai_agent_usage_stat
(
    id                 BIGSERIAL PRIMARY KEY,
    agent_id           BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    user_name          VARCHAR(255),
    department         VARCHAR(255) DEFAULT '',
    message_count      INTEGER      DEFAULT 0,
    conversation_count INTEGER      DEFAULT 0,
    stat_date          DATE         NOT NULL,
    create_dt          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_user_date UNIQUE (agent_id, user_id, stat_date)
);

DROP TRIGGER IF EXISTS trg_sai_agent_usage_stat_touch_update_dt ON sai_agent_usage_stat;
CREATE TRIGGER trg_sai_agent_usage_stat_touch_update_dt
    BEFORE UPDATE ON sai_agent_usage_stat
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_usage_agent ON sai_agent_usage_stat (agent_id);
CREATE INDEX IF NOT EXISTS idx_usage_date ON sai_agent_usage_stat (stat_date);

-- ============================================
-- MCP 服务管理
-- ============================================

-- MCP 服务配置表
CREATE TABLE IF NOT EXISTS sai_mcp_server
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)  NOT NULL,
    description     TEXT,
    transport_type  INTEGER       DEFAULT 1,
    base_uri        VARCHAR(1024),
    endpoint        VARCHAR(1024),
    command         VARCHAR(1024),
    args            TEXT,
    env_vars        TEXT,
    version         VARCHAR(32)   DEFAULT '1.0.0',
    auth_type       INTEGER       DEFAULT 0,
    auth_config     TEXT,
    status          INTEGER       DEFAULT 0,
    capabilities    TEXT,
    last_connect_dt TIMESTAMP NULL,
    creator_id      BIGINT,
    create_dt       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_mcp_server_touch_update_dt ON sai_mcp_server;
CREATE TRIGGER trg_sai_mcp_server_touch_update_dt
    BEFORE UPDATE ON sai_mcp_server
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_mcp_server_creator ON sai_mcp_server (creator_id);
CREATE INDEX IF NOT EXISTS idx_mcp_server_status ON sai_mcp_server (status);

-- 智能体与MCP服务关联表(多对多)
CREATE TABLE IF NOT EXISTS sai_agent_mcp_server
(
    id            BIGSERIAL PRIMARY KEY,
    agent_id      BIGINT    NOT NULL,
    mcp_server_id BIGINT    NOT NULL,
    create_dt     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_mcp UNIQUE (agent_id, mcp_server_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_mcp_agent ON sai_agent_mcp_server (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_mcp_server ON sai_agent_mcp_server (mcp_server_id);

-- 用户订阅的智能体（多对多）
CREATE TABLE IF NOT EXISTS sai_user_agent
(
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT    NOT NULL,
    agent_id  BIGINT    NOT NULL,
    create_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_agent UNIQUE (user_id, agent_id)
);

CREATE INDEX IF NOT EXISTS idx_user_agent_user ON sai_user_agent (user_id);

-- ============================================
-- Skill 技能包管理
-- ============================================

-- Skill 技能包表
CREATE TABLE IF NOT EXISTS sai_skill
(
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    description   TEXT,
    file_name     VARCHAR(255),
    file_path     VARCHAR(1024),
    file_size     BIGINT       DEFAULT 0,
    skill_content TEXT,
    storage_path  VARCHAR(500) DEFAULT NULL,
    version       BIGINT       DEFAULT 0,
    has_files     BOOLEAN      DEFAULT FALSE,
    creator_id    BIGINT,
    create_dt     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_skill_touch_update_dt ON sai_skill;
CREATE TRIGGER trg_sai_skill_touch_update_dt
    BEFORE UPDATE ON sai_skill
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_skill_creator ON sai_skill (creator_id);

-- Skill 支撑文件内容表
CREATE TABLE IF NOT EXISTS sai_skill_file
(
    id         BIGSERIAL PRIMARY KEY,
    skill_id   BIGINT       NOT NULL,
    file_path  VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    file_size  INTEGER      NOT NULL,
    encoding   VARCHAR(50)  DEFAULT 'utf-8',
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_skill_path UNIQUE (skill_id, file_path)
);

DROP TRIGGER IF EXISTS trg_sai_skill_file_touch_updated_at ON sai_skill_file;
CREATE TRIGGER trg_sai_skill_file_touch_updated_at
    BEFORE UPDATE ON sai_skill_file
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_updated_at();

CREATE INDEX IF NOT EXISTS idx_skill_id ON sai_skill_file (skill_id);

-- 智能体与Skill关联表(多对多)
CREATE TABLE IF NOT EXISTS sai_agent_skill
(
    id        BIGSERIAL PRIMARY KEY,
    agent_id  BIGINT    NOT NULL,
    skill_id  BIGINT    NOT NULL,
    create_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_skill UNIQUE (agent_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_skill_agent ON sai_agent_skill (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_skill_skill ON sai_agent_skill (skill_id);

-- ============================================================
CREATE TABLE IF NOT EXISTS sai_store_instance
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    category   INTEGER      NOT NULL,
    type       INTEGER      NOT NULL,
    config     TEXT         DEFAULT NULL,
    status     INTEGER      DEFAULT 1,
    is_default BOOLEAN      DEFAULT FALSE,
    create_dt  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_sai_store_instance_touch_update_dt ON sai_store_instance;
CREATE TRIGGER trg_sai_store_instance_touch_update_dt
    BEFORE UPDATE ON sai_store_instance
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_store_instance_category ON sai_store_instance (category);
CREATE INDEX IF NOT EXISTS idx_store_instance_type ON sai_store_instance (type);

-- 客户端应用
-- ----------------------------
CREATE TABLE IF NOT EXISTS sai_app
(
    id             BIGSERIAL PRIMARY KEY,
    app_id         VARCHAR(128) NOT NULL,
    app_name       VARCHAR(255) NOT NULL,
    description    VARCHAR(512),
    token          VARCHAR(128) NOT NULL,
    route_strategy VARCHAR(32)  DEFAULT 'LEAST_LOAD',
    status         INTEGER      DEFAULT 1,
    create_dt      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_id UNIQUE (app_id)
);

DROP TRIGGER IF EXISTS trg_sai_app_touch_update_dt ON sai_app;
CREATE TRIGGER trg_sai_app_touch_update_dt
    BEFORE UPDATE ON sai_app
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

-- ----------------------------
-- AI客户端实例节点
-- ----------------------------
CREATE TABLE IF NOT EXISTS sai_client_node
(
    id                  BIGSERIAL PRIMARY KEY,
    app_id              VARCHAR(128) NOT NULL,
    host_id             VARCHAR(128) NOT NULL,
    host_ip             VARCHAR(64)  NOT NULL,
    grpc_port           INTEGER      NOT NULL,
    max_concurrent      INTEGER      DEFAULT 10,
    active_chats        INTEGER      DEFAULT 0,
    supported_providers TEXT,
    labels              TEXT,
    expire_dt           TIMESTAMP    NOT NULL,
    create_dt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_client_node UNIQUE (app_id, host_id)
);

DROP TRIGGER IF EXISTS trg_sai_client_node_touch_update_dt ON sai_client_node;
CREATE TRIGGER trg_sai_client_node_touch_update_dt
    BEFORE UPDATE ON sai_client_node
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_app_expire ON sai_client_node (app_id, expire_dt);

-- OpenAPI 外部用户映射表
CREATE TABLE IF NOT EXISTS sai_openapi_user
(
    id               BIGSERIAL PRIMARY KEY,
    app_id           VARCHAR(128) NOT NULL,
    open_id          VARCHAR(64)  NOT NULL,
    platform_user_id BIGINT       NOT NULL,
    external_id      VARCHAR(256) DEFAULT NULL,
    nickname         VARCHAR(128) DEFAULT NULL,
    create_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_open UNIQUE (app_id, open_id),
    CONSTRAINT uk_app_external UNIQUE (app_id, external_id)
);

DROP TRIGGER IF EXISTS trg_sai_openapi_user_touch_update_dt ON sai_openapi_user;
CREATE TRIGGER trg_sai_openapi_user_touch_update_dt
    BEFORE UPDATE ON sai_openapi_user
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_open_id ON sai_openapi_user (open_id);
CREATE INDEX IF NOT EXISTS idx_platform_user ON sai_openapi_user (platform_user_id);

-- ----------------------------
-- 通用资源存储
-- ----------------------------
CREATE TABLE IF NOT EXISTS sai_resource
(
    id            BIGSERIAL PRIMARY KEY,
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
    CONSTRAINT uk_storage_key UNIQUE (storage_key)
);

DROP TRIGGER IF EXISTS trg_sai_resource_touch_update_dt ON sai_resource;
CREATE TRIGGER trg_sai_resource_touch_update_dt
    BEFORE UPDATE ON sai_resource
    FOR EACH ROW
    EXECUTE FUNCTION sai_touch_update_dt();

CREATE INDEX IF NOT EXISTS idx_biz ON sai_resource (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_creator ON sai_resource (creator_id);

-- ============================================
-- 初始化数据
-- ============================================

-- 默认管理员：admin / admin123
INSERT INTO sai_user (id, role, username, email, password, create_dt, update_dt)
VALUES (1, 2, 'admin', '', 'pbkdf2$120000$c25haWwtYWktYWRtaW4tMQ==$kakglT/wYKOgv/77Ah1stie58d/JbY2nGgq5DwgUBw4=',
        '2026-02-11 13:56:48.210429', '2026-02-11 13:56:48.210429')
ON CONFLICT DO NOTHING;

-- 插入常见的AI提供商
INSERT INTO sai_model_provider (provider_name, provider_key, description, is_enabled)
VALUES ('OpenAI', 'openai', 'OpenAI官方模型 (GPT-4, GPT-3.5等)', TRUE),
       ('Claude', 'claude', 'Anthropic Claude模型', TRUE),
       ('Ollama', 'ollama', '本地开源模型 (Llama, Mistral等)', TRUE),
       ('Google Gemini', 'gemini', 'Google Gemini模型', TRUE),
       ('阿里云百炼', 'qwen', '阿里云百炼 OpenAI 兼容模型 (Qwen等)', TRUE),
       ('DeepSeek', 'deepseek', 'DeepSeek OpenAI 兼容模型', TRUE),
       ('智谱AI', 'zhipu', '智谱AI OpenAI 兼容模型 (GLM等)', TRUE)
ON CONFLICT DO NOTHING;

-- 初始化测试应用
INSERT INTO sai_app (id, app_id, app_name, description, token, route_strategy, status, create_dt, update_dt)
VALUES (1, 'snail-ai-agent-demo', 'snail-ai-agent-demo', '', 'SAI_3ce13fa4e56a43c2b42e380c649629a5', 'LEAST_LOAD', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 初始化测试的openid
INSERT INTO sai_openapi_user (id, app_id, open_id, platform_user_id, external_id, nickname, create_dt, update_dt)
VALUES (1, 'snail-ai-agent-demo', '46ed53c6a20044c7bbd870848e80f92f', 1, '1', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 同步显式插入 id 后的序列位置。
SELECT setval(pg_get_serial_sequence('sai_user', 'id'), COALESCE((SELECT MAX(id) FROM sai_user), 1), TRUE);
SELECT setval(pg_get_serial_sequence('sai_app', 'id'), COALESCE((SELECT MAX(id) FROM sai_app), 1), TRUE);
SELECT setval(pg_get_serial_sequence('sai_openapi_user', 'id'), COALESCE((SELECT MAX(id) FROM sai_openapi_user), 1), TRUE);
