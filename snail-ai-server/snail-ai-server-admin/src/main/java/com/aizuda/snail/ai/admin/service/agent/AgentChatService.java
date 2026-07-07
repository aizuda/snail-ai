package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.vo.agent.AgentChatAttachmentVO;
import com.aizuda.snail.ai.feature.agent.chain.AgentChatContext;
import com.aizuda.snail.ai.feature.agent.dto.AgentChatAttachmentDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体对话服务，构建对话上下文并委托对话责任链完成流式对话。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {
    private final AgentChatChainService agentChatChainService;

    @SuppressWarnings("unused")
    private static final String DEFAULT_INSTRUCTION = "你是一个智能助手。";

    /**
     * 智能体流式对话——通过责任链依次完成：初始化上下文 → 会话管理 → 模型解析 →
     * MCP加载 → 系统提示词构建 → Skill注入 → RAG检索 → LLM调用
     *
     * @param command 智能体对话命令（智能体ID、会话ID、内容、流写入器、用户信息、禁用的 MCP/Skill 等）
     */
    public void chat(AgentChatCommand command) {
        AgentChatContext context = new AgentChatContext(
                command.getAgentId(),
                command.getConversationId(),
                command.getContent(),
                command.getStreamWriter(),
                command.getRequestUser(),
                command.getOpenId());
        context.setDisabledMcpServerIds(command.getDisabledMcpServerIds());
        context.setDisabledSkillIds(command.getDisabledSkillIds());
        context.setRequestedAttachments(toAttachmentDTOs(command.getAttachments()));
        context.setSid(command.getSid());

        log.debug("智能体对话开始 agentId={} conversationId={} sid={}",
                command.getAgentId(), command.getConversationId(), command.getSid());
        agentChatChainService.proceed(context);
    }

    private List<AgentChatAttachmentDTO> toAttachmentDTOs(List<AgentChatAttachmentVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> AgentChatAttachmentDTO.builder()
                        .resourceId(attachment.getResourceId())
                        .type(attachment.getType())
                        .build())
                .toList();
    }
}
