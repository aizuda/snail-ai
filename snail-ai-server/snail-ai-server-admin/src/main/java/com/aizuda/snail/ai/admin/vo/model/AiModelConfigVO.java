package com.aizuda.snail.ai.admin.vo.model;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI模型配置响应 VO
 * 用于返回给前端，隐藏敏感信息（API Key脱敏）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelConfigVO {

    private Long id;
    private Long providerId;
    private String providerName;
    private String modelName;
    private String modelKey;
    private String modelType;
    private String adapterKey;
    private String description;
    private String apiKey;  // 脱敏后的API Key
    private String apiEndpoint;
    private ConfigExtAttrsDTO configJson;
    private Long ownerId;
    private String scope;
    private Boolean isDefault;
    private Boolean isEnabled;
    private LocalDateTime createdDt;
    private LocalDateTime updatedDt;
}
