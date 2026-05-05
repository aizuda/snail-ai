package com.aizuda.snail.ai.common.grpc.handler;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;

/**
 * gRPC 分发层共用的响应构建。
 */
public final class GrpcDispatchResults {

    private static final int STATUS_FAILED = 0;

    private GrpcDispatchResults() {
    }

    public static GrpcSnailAiResult unknownUri(long reqId, String uri) {
        return GrpcSnailAiResult.newBuilder()
                .setReqId(reqId)
                .setStatus(STATUS_FAILED)
                .setMessage("Unknown URI: " + uri)
                .build();
    }

    public static GrpcSnailAiResult unknownUri(String uri) {
        return GrpcSnailAiResult.newBuilder()
                .setStatus(STATUS_FAILED)
                .setMessage("Unknown URI: " + uri)
                .build();
    }
}
