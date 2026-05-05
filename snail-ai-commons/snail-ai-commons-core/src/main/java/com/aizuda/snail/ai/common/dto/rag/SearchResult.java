package com.aizuda.snail.ai.common.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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
