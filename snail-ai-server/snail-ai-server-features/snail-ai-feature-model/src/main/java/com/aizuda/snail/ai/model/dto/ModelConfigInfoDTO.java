package com.aizuda.snail.ai.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigInfoDTO {
    /**
     * 配置ID
     */
    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型标识符
     */
    private String modelKey;

    /**
     * 提供商ID
     */
    private Long providerId;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 提供商标识
     */
    private String providerKey;

    /**
     * 模型类型 (CHAT/EMBEDDING/RERANKER/IMAGE/SPEECH)
     */
    private String modelType;

    /**
     * 模型底层协议适配器
     */
    private String adapterKey;

    /**
     * 描述
     */
    private String description;

    /**
     * 作用域 (GLOBAL=全局, PERSONAL=个人)
     */
    private String scope;

    /**
     * 是否为默认模型
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * API端点（可选，某些提供商可能需要自定义端点）
     */
    private String apiEndpoint;

    /**
     * 模型配置JSON（用于模型特定参数，如temperature等）
     * 仅在内部使用，不返回给外部
     */
    private ConfigExtAttrsDTO configJson;

    /**
     * 加密后的API Key（内部传输用，不对外暴露）
     */
    private String encryptedApiKey;

    /**
     * 更新时间（用于运行时缓存版本判断）
     */
    private LocalDateTime updatedDt;
}
