package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短期记忆历史查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortTermHistoryQuery {

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 智能体 ID
     */
    private Long agentId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 短期记忆滑动窗口大小
     */
    private Integer shortTermMemorySize;
}
