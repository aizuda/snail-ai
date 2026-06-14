package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.feature.agent.chain.AgentChatContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        context.setSid(command.getSid());

        agentChatChainService.proceed(context);
    }
}
