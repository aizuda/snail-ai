package com.aizuda.snail.ai.common.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 检索响应，封装一次检索命中的结果列表。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResponse {

    /**
     * 检索命中的结果列表
     */
    private List<SearchResult> results;
}
