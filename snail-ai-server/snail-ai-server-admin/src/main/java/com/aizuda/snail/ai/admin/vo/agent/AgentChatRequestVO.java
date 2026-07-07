package com.aizuda.snail.ai.admin.vo.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 智能体对话请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AgentChatRequestVO {

    /** 会话 ID */
    @NotBlank(message = "conversationId is required")
    private String conversationId;

    /** 用户发送的消息内容 */
    @NotBlank(message = "content is required")
    private String content;

    /** 本次对话携带的附件列表 */
    private List<AgentChatAttachmentVO> attachments;

    /** 本次对话临时禁用的 MCP 服务 ID 列表 */
    private List<Long> disabledMcpServerIds;

    /** 本次对话临时禁用的技能 ID 列表 */
    private List<Long> disabledSkillIds;
}
