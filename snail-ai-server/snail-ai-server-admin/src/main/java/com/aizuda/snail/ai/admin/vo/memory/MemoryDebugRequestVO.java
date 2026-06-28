package com.aizuda.snail.ai.admin.vo.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemoryDebugRequestVO {

    /** 用于检索的智能体（需在智能体上绑定本记忆配置） */
    @NotNull
    private Long agentId;

    /** 对话用户 ID（记忆实体） */
    @NotNull
    private Long userId;

    @NotBlank
    private String query;

    private String conversationId;
}
