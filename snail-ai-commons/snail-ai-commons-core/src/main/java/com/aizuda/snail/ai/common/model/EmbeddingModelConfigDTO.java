package com.aizuda.snail.ai.common.model;

import lombok.Data;

import java.util.List;

/**
 * (复制自 snail-job-ai-executor)
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
public class EmbeddingModelConfigDTO {
    private List<String> inputs;
    private String modelName;
    private Integer dimensions;
}
