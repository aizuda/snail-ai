package com.aizuda.snail.ai.features.rag.strategy.chunker;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkContext {
    /** 待切片的文档全文 */
    private String content;
    /** 单片段最大 token 数 */
    private int maxTokens;
    /** 片段间重叠 token 数 */
    private int overlap;
    /** mode=delimiter 时的自定义分隔符（可为 JSON 数组） */
    private String customDelimiter;
    /** mode=regex 时的正则表达式 */
    private String chunkRegex;
    /** mode=smart 时的对话模型配置 ID */
    private Long chunkModelId;
}
