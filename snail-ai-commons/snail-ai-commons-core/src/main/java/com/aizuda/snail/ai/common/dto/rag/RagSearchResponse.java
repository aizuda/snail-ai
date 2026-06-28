package com.aizuda.snail.ai.common.dto.rag;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResponse {

    private List<SearchResult> results;
}
