package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.feature.agent.chain.AgentChatContext;
import com.aizuda.snail.ai.feature.agent.chain.AgentChatHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 责任链执行器，持有有序的 Handler 列表，通过 proceed 逐个驱动
 */
@Component
public class AgentChatChainService {
    private final List<AgentChatHandler> handlers;
    public AgentChatChainService(List<AgentChatHandler> handlers) {
        this.handlers = handlers;
    }

    public void proceed(AgentChatContext ctx) {
        for (AgentChatHandler handler : handlers) {
            handler.handle(ctx);
        }
    }
}
