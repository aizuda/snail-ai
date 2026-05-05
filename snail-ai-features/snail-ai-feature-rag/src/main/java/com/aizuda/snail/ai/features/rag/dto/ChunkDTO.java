package com.aizuda.snail.ai.features.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChunkDTO {
    private int paragraphIndex;
    private int chunkIndex;
    private String content;
    private int tokenCount;
}
