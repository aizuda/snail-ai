package com.aizuda.snail.ai.feature.agent.callback;

import com.aizuda.snail.ai.common.dto.agent.ConversationRecordRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 回调：保存对话消息（用户/助手）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationRecordCallbackHandler implements GrpcRequestHandler {

    private final AgentConversationRecordMapper recordMapper;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CALLBACK_CONVERSATION_RECORD.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        try {
            ConversationRecordRequest req = JsonUtil.parseObject(request.getBody(), ConversationRecordRequest.class);

            recordMapper.insert(AgentConversationRecordPO.builder()
                    .agentId(req.getAgentId())
                    .conversationId(req.getConversationId())
                    .userId(req.getUserId())
                    .role(req.getRole())
                    .content(req.getContent())
                    .build());

            return GrpcSnailAiResult.newBuilder().setStatus(1).setMessage("OK").build();
        } catch (Exception e) {
            log.error("Callback conversation record failed", e);
            return GrpcSnailAiResult.newBuilder().setStatus(0).setMessage(e.getMessage()).build();
        }
    }
}
