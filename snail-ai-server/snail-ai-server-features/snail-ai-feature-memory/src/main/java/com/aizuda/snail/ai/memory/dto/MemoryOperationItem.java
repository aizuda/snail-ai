package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条记忆操作结果
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryOperationItem {

    /**
     * 记忆主键 ID
     */
    private Long memoryId;

    /**
     * 向量库中的向量 ID
     */
    private String vectorId;

    /**
     * 记忆内容
     */
    private String memory;
}
