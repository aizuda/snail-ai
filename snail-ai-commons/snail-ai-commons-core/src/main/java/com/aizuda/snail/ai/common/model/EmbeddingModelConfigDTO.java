package com.aizuda.snail.ai.common.model;

import lombok.Data;

import java.util.List;

/**
 * (复制自 snail-job-ai-executor)
 */
@Data
public class EmbeddingModelConfigDTO {
    private List<String> inputs;
    private String modelName;
    private Integer dimensions;
}
