package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * SearchDocument
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchDocument {

    private String id;

    private String content;

    private Map<String, Object> metadata;
}
