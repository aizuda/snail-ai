package com.aizuda.snail.ai.persistence.rag.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author: zhangshuguang
 * date: 2026-04-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagConfigDO {

    private SearchParams searchParams;

    private ModelParams modelParams;

    /** 文档切片策略（创建知识库时写入） */
    private ChunkParams chunkParams;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkParams {
        /** length | delimiter | regex | smart */
        private String mode;
        /** 单片段最大 token/字符量级（与 recursive splitter 一致） */
        private Integer maxChunkTokens;
        private Integer chunkOverlap;
        /** mode=delimiter 时作为一级切分符 */
        private String customDelimiter;
        /** mode=regex 时的一级切分正则（Java Pattern） */
        private String chunkRegex;
        /** mode=smart 时用于语义切片的对话模型配置 ID */
        private Long chunkModelId;
        private Boolean mergeShortSegments;
        private Boolean imageOcr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchParams {
        private Integer resultCount;
        private Boolean rerankEnabled;
        private Long rerankModelId;
        private Integer enterRerankCount;
        private Boolean thresholdEnabled;
        private Double threshold;
        private Double denseWeight;
        private String fusionStrategy;
        private Integer rrfK;
        private Boolean questionRewrite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelParams {
        private Long modelId;
        private Integer nearbySliceCount;
        private String prompt;
    }
}
