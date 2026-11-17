package com.aizuda.snail.ai.admin.vo.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 智能体系统提示词优化请求。 */
@Data
public class AgentInstructionOptimizeRequestVO {

    /** 智能体名称。 */
    @NotBlank(message = "name is required")
    private String name;

    /** 智能体用途描述。 */
    private String description;

    /** 当前系统提示词。 */
    @NotBlank(message = "instruction is required")
    private String instruction;

    /** 可选的优化方向。 */
    private String optimizeInstruction;
}
