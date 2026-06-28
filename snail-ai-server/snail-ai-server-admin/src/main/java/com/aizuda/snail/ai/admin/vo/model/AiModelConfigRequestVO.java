package com.aizuda.snail.ai.admin.vo.model;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AI模型配置请求 VO
 * 用于接收前端新增/编辑模型配置的请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelConfigRequestVO {

    /**
     * 提供商ID (必填)
     */
    @NotNull(message = "提供商ID不能为空")
    private Long providerId;

    /**
     * 模型名称 (必填)
     */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /**
     * 模型标识符 (必填)
     */
    @NotBlank(message = "模型标识符不能为空")
    private String modelKey;

    /**
     * 模型类型 (必填)
     * CHAT, EMBEDDING, RERANKER, IMAGE, SPEECH
     */
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    /**
     * 模型底层协议适配器
     */
    private String adapterKey;

    /**
     * 模型描述
     */
    private String description;

    /**
     * API密钥 (必填)
     */
    private String apiKey;

    /**
     * API端点URL (必填)
     */
    @NotBlank(message = "API端点不能为空")
    private String apiEndpoint;

    /**
     * 模型参数配置 (JSON格式)
     */
    private ConfigExtAttrsDTO configJson;

    /**
     * 所有者ID (可选)
     * NULL: 全局模型
     * 具体值: 用户个人模型
     */
    private Long ownerId;

    /**
     * 作用域 (可选，默认GLOBAL)
     * GLOBAL, PERSONAL
     */
    private String scope;

    /**
     * 是否为默认模型 (可选，默认false)
     */
    private Boolean isDefault;
}
