package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 智能体系统提示词优化结果。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInstructionOptimizeResponseVO {

    /** 优化后的候选提示词；失败时为原提示词。 */
    private String instruction;

    /** 非阻断 warning 代码。 */
    private List<String> warnings;
}
