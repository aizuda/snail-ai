package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆处理指标，统计一次记忆抽取/写入的耗时与各类操作数量。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMetrics {

    /**
     * 记忆抽取耗时（毫秒）
     */
    private Long extractionMs;
    /**
     * 候选记忆数量
     */
    private Integer candidateCount;
    /**
     * 新增记忆数量
     */
    private Integer addedCount;
    /**
     * 更新记忆数量
     */
    private Integer updatedCount;
    /**
     * 删除记忆数量
     */
    private Integer deletedCount;
    /**
     * 跳过（未变更）记忆数量
     */
    private Integer skippedCount;
}
