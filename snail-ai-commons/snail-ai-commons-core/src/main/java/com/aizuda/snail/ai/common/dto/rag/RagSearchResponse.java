package com.aizuda.snail.ai.common.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RagSearchResponse
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResponse {

    private List<SearchResult> results;
}
