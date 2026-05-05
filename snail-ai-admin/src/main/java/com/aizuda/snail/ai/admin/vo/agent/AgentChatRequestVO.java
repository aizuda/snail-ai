package com.aizuda.snail.ai.admin.vo.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AgentChatRequestVO {

    @NotBlank(message = "conversationId is required")
    private String conversationId;

    @NotBlank(message = "content is required")
    private String content;

    private List<Long> disabledMcpServerIds;

    private List<Long> disabledSkillIds;
}
