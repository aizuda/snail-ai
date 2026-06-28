package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * VectorSearchResult
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorSearchResult {

    private String id;

    private String content;

    private float score;

    private Map<String, Object> metadata;
}
