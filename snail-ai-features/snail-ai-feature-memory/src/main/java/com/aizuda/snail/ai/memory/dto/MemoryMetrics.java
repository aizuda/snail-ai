package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMetrics {

    private Long extractionMs;
    private Integer candidateCount;
    private Integer addedCount;
    private Integer updatedCount;
    private Integer deletedCount;
    private Integer skippedCount;
}
