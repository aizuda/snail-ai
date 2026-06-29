package com.aizuda.snail.ai.admin.vo.rag;

import lombok.Data;

/**
 * RAG 切片更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagChunkUpdateRequestVO {

    /** 切片内容 */
    private String content;
}
