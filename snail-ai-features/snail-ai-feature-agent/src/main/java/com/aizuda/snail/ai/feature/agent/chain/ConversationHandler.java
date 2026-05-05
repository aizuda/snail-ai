package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.enums.agent.StatusEnum;
import com.aizuda.snail.ai.persistence.agent.enums.ConversationRoleEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 会话管理：创建或复用对话记录，持久化用户消息
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class ConversationHandler implements AgentChatHandler {

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationRecordMapper recordMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }
        Long agentId = ctx.getAgentId();
        String conversationId = ctx.getConversationId();
        String content = ctx.getContent();
        Long userId = ctx.getUser().getId();

        Long convCount = conversationMapper.selectCount(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getConversationId, conversationId));
        if (convCount == 0) {
            conversationMapper.insert(AgentConversationPO.builder()
                    .agentId(agentId)
                    .userId(userId)
                    .conversationId(conversationId)
                    .title(content.substring(0, Math.min(content.length(), 16)))
                    .build());
        }

        recordMapper.insert(AgentConversationRecordPO.builder()
                .agentId(agentId)
                .conversationId(conversationId)
                .userId(userId)
                .role(ConversationRoleEnum.USER.getValue())
                .content(content)
                .status(StatusEnum.RUNNING.getValue())
                .build());

    }
}
