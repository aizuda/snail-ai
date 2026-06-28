package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentUsageDetailVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentUsageDetailVO {

    private Long userId;

    private String userName;

    private Integer messageCount;
}
