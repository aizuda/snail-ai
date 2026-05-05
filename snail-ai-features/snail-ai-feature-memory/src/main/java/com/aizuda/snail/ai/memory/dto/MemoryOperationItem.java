package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条记忆操作结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryOperationItem {

    private Long memoryId;

    private String vectorId;

    private String memory;
}
