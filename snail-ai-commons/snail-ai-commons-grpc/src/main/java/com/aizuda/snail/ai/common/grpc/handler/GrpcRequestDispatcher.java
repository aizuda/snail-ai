package com.aizuda.snail.ai.common.grpc.handler;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * gRPC 请求统一分发器 — Server / Client 共用，按 {@link GrpcRequestHandler#supports(String)} 路由。
 *
 * @author opensnail
 */
@Component
@RequiredArgsConstructor
public class GrpcRequestDispatcher {

    private final List<GrpcRequestHandler> unaryHandlers;
    private final List<GrpcStreamingRequestHandler> streamingHandlers;

    public GrpcSnailAiResult dispatchUnary(long reqId, String uri, Map<String, String> headers, String body) {
        GrpcHandlerRequest request = GrpcHandlerRequest.builder()
                .reqId(reqId).uri(uri).headers(headers).body(body).build();

        for (GrpcRequestHandler handler : unaryHandlers) {
            if (handler.supports(uri)) {
                return handler.handle(request).toBuilder().setReqId(reqId).build();
            }
        }
        return GrpcDispatchResults.unknownUri(reqId, uri);
    }

    public void dispatchStreaming(String uri, Map<String, String> headers, String body,
                                  long reqId, StreamObserver<GrpcSnailAiResult> observer) {
        GrpcHandlerRequest request = GrpcHandlerRequest.builder()
                .reqId(reqId).uri(uri).headers(headers).body(body).build();

        for (GrpcStreamingRequestHandler handler : streamingHandlers) {
            if (handler.supports(uri)) {
                handler.handle(request, observer);
                return;
            }
        }
        observer.onNext(GrpcDispatchResults.unknownUri(reqId, uri));
        observer.onCompleted();
    }
}
