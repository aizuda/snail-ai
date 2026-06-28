package com.aizuda.snail.ai.common.dto.rag;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResult {
    private String id;
    private Long chunkId;
    private String content;
    private double score;
    private Long documentId;
    private String documentName;
    private Map<String, Object> metadata;
}
