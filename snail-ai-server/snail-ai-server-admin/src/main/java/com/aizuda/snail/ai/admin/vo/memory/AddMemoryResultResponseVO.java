package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.memory.dto.MemoryMetrics;
import com.aizuda.snail.ai.memory.dto.MemoryOperationItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemoryResultResponseVO {

    private List<MemoryOperationItem> operations;
    private MemoryMetrics metrics;
}
