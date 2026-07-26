package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 记忆变更历史项响应对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryHistoryItemResponseVO {

    /** 变更事件类型（新增/更新/删除） */
    private Integer event;
    /** 变更前的记忆内容 */
    private String oldMemory;
    /** 变更后的记忆内容 */
    private String newMemory;
    /** 变更发生时间 */
    private LocalDateTime timestamp;
}
