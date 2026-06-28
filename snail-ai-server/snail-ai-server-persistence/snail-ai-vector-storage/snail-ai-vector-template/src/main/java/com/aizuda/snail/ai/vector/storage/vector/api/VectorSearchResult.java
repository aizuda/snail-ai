package com.aizuda.snail.ai.vector.storage.vector.api;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
