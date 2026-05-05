package com.aizuda.snail.ai.admin.vo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI模型提供商 VO
 * 用于返回提供商信息给前端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelProviderVO {

    /**
     * 提供商ID
     */
    private Long id;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 提供商标识符
     */
    private String providerKey;

    /**
     * 提供商描述
     */
    private String description;

    /**
     * LOGO图标URL
     */
    private String iconUrl;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 创建时间戳 (毫秒)
     */
    private LocalDateTime createdDt;

    /**
     * 更新时间戳 (毫秒)
     */
    private LocalDateTime updatedDt;
}
