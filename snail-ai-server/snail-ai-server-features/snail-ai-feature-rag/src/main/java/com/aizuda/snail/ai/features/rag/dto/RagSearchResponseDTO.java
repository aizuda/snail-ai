package com.aizuda.snail.ai.features.rag.dto;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 检索响应：命中结果列表与各阶段耗时 / 命中量指标。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagSearchResponseDTO {

    /**
     * 命中结果列表
     */
    private List<SearchResult> results;

    /**
     * 各阶段耗时与命中量指标
     */
    private RagStageMetricsDTO metrics;
}
