package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.memory.dto.MemoryMetrics;
import com.aizuda.snail.ai.memory.dto.MemoryOperationItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新增记忆结果响应对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemoryResultResponseVO {

    /** 记忆操作明细列表（新增/更新/删除） */
    private List<MemoryOperationItem> operations;
    /** 记忆操作指标统计 */
    private MemoryMetrics metrics;
}
