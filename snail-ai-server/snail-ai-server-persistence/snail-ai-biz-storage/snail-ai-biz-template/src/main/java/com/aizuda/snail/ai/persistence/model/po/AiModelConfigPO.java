package com.aizuda.snail.ai.persistence.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI模型配置持久化对象
 * 表: sai_model_config
 *
 * 支持多提供商、多模型类型的灵活配置
 * 支持全局配置(owner_id=null)和个人配置(owner_id=userId)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sai_model_config")
public class AiModelConfigPO {

    /**
     * 模型配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提供商ID (外键)
     * 关联到 sai_model_provider.id
     */
    private Long providerId;

    /**
     * 模型名称
     * 例如: gpt-4, claude-3-opus, llama2
     */
    private String modelName;

    /**
     * 模型标识符 (唯一)
     * 例如: gpt-4, claude-opus-3, llama2-13b
     */
    private String modelKey;

    /**
     * 模型类型
     * CHAT: 对话模型
     * EMBEDDING: 向量模型
     * RERANKER: 重排模型
     * IMAGE: 图像模型
     * SPEECH: 语音模型
     */
    private String modelType;

    /**
     * 模型底层协议适配器
     * CHAT/EMBEDDING 默认 openai-compatible，RERANKER 默认 http
     */
    private String adapterKey;

    /**
     * 模型描述
     */
    private String description;

    /**
     * API密钥 (加密存储)
     */
    private String apiKey;

    /**
     * API端点URL
     */
    private String apiEndpoint;

    /**
     * 模型参数配置 (JSONB格式)
     * 例如:
     * {
     *   "temperature": 0.7,
     *   "maxTokens": 2000,
     *   "topP": 0.9,
     *   "timeoutMs": 30000
     * }
     */
    private String configJson;

    /**
     * 所有者ID
     * NULL: 全局模型 (Admin配置)
     * 具体值: 用户ID (个人配置)
     */
    private Long ownerId;

    /**
     * 作用域
     * GLOBAL: 全局 (Admin配置)
     * PERSONAL: 个人 (用户配置)
     */
    private String scope;

    /**
     * 是否为该模型类型的默认模型
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdDt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDt;
}
