package com.aizuda.snail.ai.admin.vo.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库配置请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeConfigRequestVO {

    /** 检索参数 */
    private SearchParams searchParams;

    /** 模型参数 */
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
        /** 切片重叠量 */
        private Integer chunkOverlap;
        /** mode=delimiter 时作为一级切分符 */
        private String customDelimiter;
        /** mode=regex 时的一级切分正则（Java Pattern） */
        private String chunkRegex;
        /** mode=smart 时用于语义切片的对话模型配置 ID */
        private Long chunkModelId;
        /** 是否合并过短片段 */
        private Boolean mergeShortSegments;
        /** 是否开启图片 OCR */
        private Boolean imageOcr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchParams {
        /** 返回结果数量 */
        private Integer resultCount;
        /** 是否启用重排 */
        private Boolean rerankEnabled;
        /** 重排模型 ID */
        private Long rerankModelId;
        /** 进入重排的候选数量 */
        private Integer enterRerankCount;
        /** 是否启用相似度阈值 */
        private Boolean thresholdEnabled;
        /** 相似度阈值 */
        private Double threshold;
        /** 稠密向量权重 */
        private Double denseWeight;
        /** 融合策略 */
        private String fusionStrategy;
        /** RRF 融合参数 K */
        private Integer rrfK;
        /** 是否启用问题改写（原 QaParams 独有字段） */
        private Boolean questionRewrite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelParams {
        /** 对话模型 ID */
        private Long modelId;
        /** 邻近切片数量 */
        private Integer nearbySliceCount;
        /** 提示词 */
        private String prompt;
    }
}
