package com.aizuda.snail.ai.agent.core;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestDispatcher;
import io.grpc.stub.ServerCalls;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 客户端 gRPC 请求分发器 — 委托 {@link GrpcRequestDispatcher} 按 URI 路由。
 */
@RequiredArgsConstructor
public class ClientRequestDispatcher {

    private final GrpcRequestDispatcher grpcRequestDispatcher;

    /**
     * Unary handler（如 /ping）
     */
    public ServerCalls.UnaryMethod<GrpcSnailAiRequest, GrpcSnailAiResult> unaryHandler() {
        return (request, observer) -> {
            String uri = request.getMetadata().getUri();
            Map<String, String> headers = request.getMetadata().getHeadersMap();
            GrpcSnailAiResult result = grpcRequestDispatcher.dispatchUnary(
                    request.getReqId(), uri, headers, request.getBody());
            observer.onNext(result);
            observer.onCompleted();
        };
    }

    /**
     * Server-Streaming handler（如 /chat/dispatch）
     */
    public ServerCalls.ServerStreamingMethod<GrpcSnailAiRequest, GrpcSnailAiResult> streamingHandler() {
        return (request, observer) -> {
            String uri = request.getMetadata().getUri();
            Map<String, String> headers = request.getMetadata().getHeadersMap();
            grpcRequestDispatcher.dispatchStreaming(
                    uri, headers, request.getBody(), request.getReqId(), observer);
        };
    }
}
