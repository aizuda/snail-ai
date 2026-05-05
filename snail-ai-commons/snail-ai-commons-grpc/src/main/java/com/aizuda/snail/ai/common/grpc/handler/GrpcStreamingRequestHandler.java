package com.aizuda.snail.ai.common.grpc.handler;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import io.grpc.stub.StreamObserver;

/**
 * gRPC Server-Streaming 请求处理器。
 *
 * @author opensnail
 */
public interface GrpcStreamingRequestHandler {

    boolean supports(String uri);

    void handle(GrpcHandlerRequest request, StreamObserver<GrpcSnailAiResult> observer);
}
