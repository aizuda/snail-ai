package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 初始化：加载用户与智能体
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class InitContextHandler implements AgentChatHandler {

    private final AgentMapper agentMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        UserPO user = ctx.getRequestUser() != null ? ctx.getRequestUser() : UserSessionUtils.currentUserSession();
        AgentPO agent = agentMapper.selectById(ctx.getAgentId());
        if (agent == null) {
            ctx.getStreamWriter().send("Error: Agent not found: " + ctx.getAgentId());
            ctx.getStreamWriter().complete();
            ctx.setTerminated(true);
            return;
        }

        ctx.setUser(user);
        ctx.setAgent(agent);
    }
}
