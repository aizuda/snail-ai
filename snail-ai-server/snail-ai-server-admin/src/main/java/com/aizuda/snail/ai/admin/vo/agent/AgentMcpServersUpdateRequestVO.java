package com.aizuda.snail.ai.admin.vo.agent;

import lombok.Data;

import java.util.List;

/**
 * 智能体 MCP 服务绑定更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AgentMcpServersUpdateRequestVO {

    /** 绑定的 MCP 服务 ID 列表 */
    private List<Long> mcpServerIds;
}
