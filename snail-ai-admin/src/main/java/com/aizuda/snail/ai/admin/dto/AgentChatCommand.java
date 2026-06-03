package com.aizuda.snail.ai.admin.dto;

import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

/**
 * 智能体对话命令对象
 */
@Data
@Builder
public class AgentChatCommand {

    private Long agentId;
    private String conversationId;
    private String content;
    private List<Long> disabledMcpServerIds;
    private List<Long> disabledSkillIds;
    private ResponseBodyEmitter emitter;
    private UserPO requestUser;
    private String openId;
    /**
     * 提交ID，用于区分同一用户在同一对话中的并发请求
     */
    private String sid;
}
