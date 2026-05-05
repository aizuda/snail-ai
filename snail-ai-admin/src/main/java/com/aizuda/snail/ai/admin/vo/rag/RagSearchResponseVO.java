package com.aizuda.snail.ai.admin.vo.rag;

import com.aizuda.snail.ai.features.rag.dto.RagStageMetricsDTO;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagSearchResponseVO {

    private List<SearchResult> results;

    private RagStageMetricsDTO metrics;
}
