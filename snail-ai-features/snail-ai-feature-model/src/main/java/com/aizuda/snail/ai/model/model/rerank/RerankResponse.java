package com.aizuda.snail.ai.model.model.rerank;

import lombok.Data;

import java.util.List;

/**
 * 重排模型响应
 */
@Data
public class RerankResponse {
    private List<RerankResult> results;
    private long costTimeMs;

    @Data
    public static class RerankResult {
        /** 原始文档在输入列表中的索引 */
        private int index;
        /** 重排分数 */
        private double score;
    }
}
