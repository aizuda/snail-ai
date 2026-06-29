package com.aizuda.snail.ai.common.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索请求，指定目标知识库与查询文本。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchRequest {

    /**
     * 目标知识库 ID
     */
    private Long ragId;

    /**
     * 查询文本
     */
    private String query;
}
