package com.aizuda.snail.ai.features.rag.dto;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagSearchResponseDTO {

    private List<SearchResult> results;

    private RagStageMetricsDTO metrics;
}
