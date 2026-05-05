package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.feature.agent.chain.AgentChatContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        // 生成 traceId 和 root SPAN ID
        String traceId = UUID.randomUUID().toString();
        String rootSpanId = UUID.randomUUID().toString();
        long rootSpanStart = System.currentTimeMillis();

        AgentChatContext context = new AgentChatContext(
                command.getAgentId(),
                command.getConversationId(),
                command.getContent(),
                command.getEmitter(),
                command.getRequestUser(),
                command.getOpenId());
        context.setDisabledMcpServerIds(command.getDisabledMcpServerIds());
        context.setDisabledSkillIds(command.getDisabledSkillIds());
        context.setTraceId(traceId);
        context.setRootSpanId(rootSpanId);
        context.setRootSpanStartTimeMs(rootSpanStart);
        log.info("[OBS_TIMING][T1] root span started: traceId={}, rootSpanId={}, conversationId={}, ts={}",
                traceId, rootSpanId, command.getConversationId(), rootSpanStart);

        try {
            agentChatChainService.proceed(context);
            log.info("[OBS_TIMING][T6] chain returned before stream complete: traceId={}, rootSpanId={}, streamDispatchStarted={}, ts={}",
                    traceId, rootSpanId, context.isStreamDispatchStarted(), System.currentTimeMillis());
        } catch (Exception e) {
            throw e;
        }

    }
}
