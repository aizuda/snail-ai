package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短期记忆历史查询参数
 *
 * @author opensnail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortTermHistoryQuery {

    private String conversationId;

    private Long agentId;

    private Long userId;

    private Integer shortTermMemorySize;
}
