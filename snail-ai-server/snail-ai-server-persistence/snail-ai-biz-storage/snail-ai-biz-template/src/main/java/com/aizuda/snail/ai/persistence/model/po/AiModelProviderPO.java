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
 * AI模型提供商信息持久化对象
 * 表: sai_model_provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sai_model_provider")
public class AiModelProviderPO {

    /**
     * 提供商ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提供商名称 (唯一)
     * 例如: OpenAI, Claude, Ollama
     */
    private String providerName;

    /**
     * 提供商标识符 (唯一)
     * 例如: openai, claude, ollama
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
     * 创建时间
     */
    private LocalDateTime createdDt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDt;
}
