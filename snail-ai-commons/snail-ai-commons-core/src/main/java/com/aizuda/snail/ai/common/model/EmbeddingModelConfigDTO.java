package com.aizuda.snail.ai.common.model;

import java.util.List;
import lombok.Data;

/** (复制自 snail-job-ai-executor) */
@Data
public class EmbeddingModelConfigDTO {
    private List<String> inputs;
    private String modelName;
    private Integer dimensions;
}
