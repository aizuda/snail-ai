package com.aizuda.snail.ai.agent.core.grpc.handler;

import com.aizuda.snail.ai.agent.common.counter.ActiveChatCounter;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 客户端 Ping：{@link UriConstants#PING}
 */
@RequiredArgsConstructor
public class PingRequestHandler implements GrpcRequestHandler {

    private static final int REQUEST_STATUS_SUCCESS = 1;

    private final ActiveChatCounter activeChatCounter;

    @Override
    public boolean supports(String uri) {
        return UriConstants.PING.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        PingResponse response = PingResponse.builder()
                .timestamp(System.currentTimeMillis())
                .activeChats(activeChatCounter.get())
                .build();
        
        return GrpcSnailAiResult.newBuilder()
                .setStatus(REQUEST_STATUS_SUCCESS)
                .setData(JsonUtil.toJsonString(response))
                .build();
    }

    /**
     * Ping 响应数据
     */
    @Data
    @Builder
    private static class PingResponse {
        private Long timestamp;
        private Integer activeChats;
    }
}
