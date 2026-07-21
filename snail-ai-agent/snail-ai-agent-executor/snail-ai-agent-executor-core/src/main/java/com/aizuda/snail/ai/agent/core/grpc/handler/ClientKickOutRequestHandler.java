package com.aizuda.snail.ai.agent.core.grpc.handler;

import com.aizuda.snail.ai.agent.common.handler.ClientHeartbeatScheduler;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端踢出：{@link UriConstants#CLIENT_KICK_OUT}
 *
 * @author opensnail
 * @since 2026-07-20
 */
@Slf4j
@RequiredArgsConstructor
public class ClientKickOutRequestHandler implements GrpcRequestHandler {

    private static final int REQUEST_STATUS_SUCCESS = 1;
    private static final String RESPONSE_MESSAGE_OK = "OK";

    private final ClientHeartbeatScheduler heartbeatScheduler;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CLIENT_KICK_OUT.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        heartbeatScheduler.pauseByKickOut();
        log.warn("Client kicked out by server, heartbeat paused");
        return GrpcSnailAiResult.newBuilder()
                .setStatus(REQUEST_STATUS_SUCCESS)
                .setMessage(RESPONSE_MESSAGE_OK)
                .build();
    }
}
