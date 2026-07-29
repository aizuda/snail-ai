package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.feature.agent.chain.AgentChatContext;
import com.aizuda.snail.ai.feature.agent.chain.AgentChatHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 责任链执行器，持有有序的 Handler 列表，通过 proceed 逐个驱动
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
public class AgentChatChainService {
    private final List<AgentChatHandler> handlers;

    /**
     * 构造责任链执行器
     *
     * @param handlers 有序的对话处理器列表（由 Spring 注入）
     */
    public AgentChatChainService(List<AgentChatHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 依次驱动责任链上的所有处理器
     *
     * @param ctx 智能体对话上下文
     */
    public void proceed(AgentChatContext ctx) {
        log.debug("对话责任链执行 handler数={}", handlers.size());
        for (AgentChatHandler handler : handlers) {
            if (ctx.isTerminated()) {
                break;
            }
            handler.handle(ctx);
        }
    }
}
