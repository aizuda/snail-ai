package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryHistoryItemResponseVO {

    private Integer event;
    private String oldMemory;
    private String newMemory;
    private LocalDateTime timestamp;
}
