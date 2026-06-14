package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 记忆统计VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemoryStatsVO {

    private Integer totalMemories;
    private Map<String, Integer> byType;  // {FACT: 45, DECISION: 30, ...}
    private Map<Long, Integer> mostUsed;  // {memoryId: accessCount}
    private Double retrievalEffectiveness;  // 检索有效率
}
