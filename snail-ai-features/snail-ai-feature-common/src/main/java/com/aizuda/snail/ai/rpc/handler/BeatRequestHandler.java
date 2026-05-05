package com.aizuda.snail.ai.rpc.handler;

import com.aizuda.snail.ai.ClientInstanceManager;
import com.aizuda.snail.ai.common.dto.beat.HeartbeatBodyRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.dto.ServerTimestampResponse;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客户端心跳：{@link UriConstants#BEAT}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BeatRequestHandler implements GrpcRequestHandler {

    private static final int REQUEST_STATUS_SUCCESS = 1;
    private static final int REQUEST_STATUS_FAILED = 0;
    private static final String RESPONSE_MESSAGE_OK = "OK";

    private static final int DEFAULT_MAX_CONCURRENT = 10;
    private static final int DEFAULT_ACTIVE_CHATS = 0;

    private final ClientInstanceManager instanceManager;

    @Override
    public boolean supports(String uri) {
        return UriConstants.BEAT.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        String appId = request.getAppId();
        String token = request.getToken();

        if (!instanceManager.validateToken(appId, token)) {
            return GrpcSnailAiResult.newBuilder()
                    .setStatus(REQUEST_STATUS_FAILED)
                    .setMessage("Token validation failed for app: " + appId)
                    .build();
        }

        ClientInstanceManager.ClientRegistration registration = buildRegistration(request);
        instanceManager.registerOrUpdate(registration);

        log.debug("Client heartbeat received: appId={}, hostId={}, activeChats={}/{}",
                 appId, registration.getHostId(), registration.getActiveChats(), registration.getMaxConcurrent());

        ServerTimestampResponse response = ServerTimestampResponse.builder()
                .serverTimestamp(System.currentTimeMillis())
                .build();

        return GrpcSnailAiResult.newBuilder()
                .setStatus(REQUEST_STATUS_SUCCESS)
                .setMessage(RESPONSE_MESSAGE_OK)
                .setData(JsonUtil.toJsonString(response))
                .build();
    }

    private ClientInstanceManager.ClientRegistration buildRegistration(GrpcHandlerRequest request) {
        HeartbeatBodyRequest body = JsonUtil.parseObject(request.getBody(), HeartbeatBodyRequest.class);
        int maxConcurrent = body != null ? body.getMaxConcurrentChats() : DEFAULT_MAX_CONCURRENT;
        int activeChats = body != null ? body.getActiveChats() : DEFAULT_ACTIVE_CHATS;

        ClientInstanceManager.ClientRegistration registration = new ClientInstanceManager.ClientRegistration();
        registration.setAppId(request.getAppId());
        registration.setHostId(request.getHostId());
        registration.setHostIp(request.getHostIp());
        registration.setGrpcPort(request.getHostPort());
        registration.setMaxConcurrent(maxConcurrent);
        registration.setActiveChats(activeChats);
        registration.setSupportedProviders(null);
        registration.setLabels(null);

        return registration;
    }
}
