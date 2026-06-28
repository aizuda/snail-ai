package com.aizuda.snail.ai.admin.vo.agent;

import java.util.List;
import lombok.Data;

@Data
public class AgentMcpServersUpdateRequestVO {

    private List<Long> mcpServerIds;
}
