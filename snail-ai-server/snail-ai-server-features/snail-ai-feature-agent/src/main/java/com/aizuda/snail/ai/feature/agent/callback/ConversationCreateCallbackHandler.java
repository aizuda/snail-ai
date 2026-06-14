package com.aizuda.snail.ai.feature.agent.callback;

import com.aizuda.snail.ai.common.dto.agent.ConversationCreateRequest;
import com.aizuda.snail.ai.common.enums.agent.StatusEnum;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.agent.enums.ConversationRoleEnum;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 回调：创建对话 + 保存用户消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationCreateCallbackHandler implements GrpcRequestHandler {

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationRecordMapper recordMapper;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CALLBACK_CONVERSATION_CREATE.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        try {
            ConversationCreateRequest req = JsonUtil.parseObject(request.getBody(), ConversationCreateRequest.class);

            // 创建对话（如不存在）
            long count = conversationMapper.selectCount(
                    new LambdaQueryWrapper<AgentConversationPO>()
                            .eq(AgentConversationPO::getConversationId, req.getConversationId()));
            if (count == 0) {
                String content = req.getUserMessage();
                String title = content != null && content.length() > 16
                        ? content.substring(0, 16) : content;
                conversationMapper.insert(AgentConversationPO.builder()
                        .agentId(req.getAgentId()).userId(req.getUserId())
                        .conversationId(req.getConversationId()).title(title)
                        .build());
            }

            // 保存用户消息
            recordMapper.insert(AgentConversationRecordPO.builder()
                    .agentId(req.getAgentId()).conversationId(req.getConversationId()).userId(req.getUserId())
                    .role(ConversationRoleEnum.USER.getValue())
                    .content(req.getUserMessage())
                    .status(StatusEnum.RUNNING.getValue())
                    .build());

            return buildSuccess();
        } catch (Exception e) {
            log.error("Callback conversation create failed", e);
            return buildError(e.getMessage());
        }
    }

    private GrpcSnailAiResult buildSuccess() {
        return GrpcSnailAiResult.newBuilder().setStatus(1).setMessage("OK").build();
    }

    private GrpcSnailAiResult buildError(String msg) {
        return GrpcSnailAiResult.newBuilder().setStatus(0).setMessage(msg).build();
    }
}
