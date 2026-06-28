package com.aizuda.snail.ai.admin.vo.agent;

import lombok.Data;

import java.util.List;

/**
 * AgentMcpServersUpdateRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AgentMcpServersUpdateRequestVO {

    private List<Long> mcpServerIds;
}
