package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

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
            try {
                ctx.getEmitter().send("Error: Agent not found: " + ctx.getAgentId(), MediaType.TEXT_PLAIN);
                ctx.getEmitter().complete();
            } catch (IOException e) {
                log.error("写入错误信息失败", e);
            }
            ctx.setTerminated(true);
            return;
        }

        ctx.setUser(user);
        ctx.setAgent(agent);
        // traceId 和 rootSpanId 已由 AgentChatService 生成并设置
        // 这里只生成 context_preparation SPAN ID，供后续 Handler 记录子观测
        ctx.setContextPreparationSpanId(UUID.randomUUID().toString());
    }
}
