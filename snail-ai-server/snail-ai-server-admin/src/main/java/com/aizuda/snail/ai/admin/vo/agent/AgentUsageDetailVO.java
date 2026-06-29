package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体使用明细（按用户统计）
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentUsageDetailVO {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String userName;

    /** 消息数量 */
    private Integer messageCount;
}
