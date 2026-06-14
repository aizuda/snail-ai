-- ============================================================
-- Snail AI 模型适配器字段升级脚本
-- 使用：mysql -u user -p database < sai_model_adapter.sql
-- ============================================================

ALTER TABLE sai_model_config
    ADD COLUMN adapter_key VARCHAR(100) COMMENT '底层协议适配器标识(openai-compatible/http等)' AFTER model_type;

UPDATE sai_model_config
SET adapter_key = 'openai-compatible'
WHERE adapter_key IS NULL
  AND model_type IN ('CHAT', 'EMBEDDING');

UPDATE sai_model_config
SET adapter_key = 'http'
WHERE adapter_key IS NULL
  AND model_type = 'RERANKER';
