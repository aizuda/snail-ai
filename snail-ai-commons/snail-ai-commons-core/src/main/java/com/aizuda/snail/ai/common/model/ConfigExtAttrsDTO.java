package com.aizuda.snail.ai.common.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型扩展配置属性
 * <p>
 * 按模型类型分组：
 * - 通用：timeoutMs
 * - CHAT：temperature, topP, topK, maxTokens, frequencyPenalty, presencePenalty, stopSequences, seed, responseFormat, stream, extraBody
 * - EMBEDDING：embeddingDimension, encodingFormat
 * - RERANKER：rerankPath
 *
 * @author opensnail
 * @date 2026-03-04
 */
@Data
public class ConfigExtAttrsDTO {

    // ==================== 通用配置 ====================

    private Long timeoutMs;

    // ==================== CHAT 对话模型 ====================

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private List<String> stopSequences;
    private Long seed;
    private String responseFormat;
    private Boolean stream;
    private Map<String, Object> extraBody;

    // ==================== EMBEDDING 向量模型 ====================

    private Integer embeddingDimension;
    private String encodingFormat;

    // ==================== RERANKER 重排模型 ====================

    private String rerankPath;
}
