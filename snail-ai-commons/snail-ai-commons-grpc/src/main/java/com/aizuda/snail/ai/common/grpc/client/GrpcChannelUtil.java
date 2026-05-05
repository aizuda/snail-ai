package com.aizuda.snail.ai.common.grpc.client;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.auto.Metadata;
import com.aizuda.snail.ai.common.grpc.constant.GrpcConstants;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * gRPC 客户端调用工具
 */
@Slf4j
public final class GrpcChannelUtil {

    private static final AtomicLong REQ_ID_GEN = new AtomicLong(0);

    private GrpcChannelUtil() {}

    /**
     * 发送 Unary 请求（同步阻塞，默认 CallOptions）
     */
    public static GrpcSnailAiResult sendUnary(ManagedChannel channel, String uri, String body,
                                               Map<String, String> headers) {
        return sendUnary(channel, uri, body, headers, CallOptions.DEFAULT);
    }

    /**
     * 发送 Unary 请求（同步阻塞，自定义 CallOptions）
     */
    public static GrpcSnailAiResult sendUnary(ManagedChannel channel, String uri, String body,
                                               Map<String, String> headers, CallOptions callOptions) {
        GrpcSnailAiRequest request = buildRequest(uri, body, headers);

        MethodDescriptor<GrpcSnailAiRequest, GrpcSnailAiResult> md =
                buildMethodDescriptor(MethodDescriptor.MethodType.UNARY,
                        GrpcConstants.UNARY_SERVICE_NAME, GrpcConstants.UNARY_METHOD_NAME);

        return ClientCalls.blockingUnaryCall(
                channel.newCall(md, callOptions), request);
    }

    /**
     * 发送 Server Streaming 请求（异步回调）
     */
    public static void sendServerStreaming(ManagedChannel channel, String uri, String body,
                                           Map<String, String> headers,
                                           StreamObserver<GrpcSnailAiResult> responseObserver) {
        GrpcSnailAiRequest request = buildRequest(uri, body, headers);

        MethodDescriptor<GrpcSnailAiRequest, GrpcSnailAiResult> md =
                buildMethodDescriptor(MethodDescriptor.MethodType.SERVER_STREAMING,
                        GrpcConstants.STREAMING_SERVICE_NAME, GrpcConstants.STREAMING_METHOD_NAME);

        ClientCalls.asyncServerStreamingCall(
                channel.newCall(md, CallOptions.DEFAULT), request, responseObserver);
    }

    private static GrpcSnailAiRequest buildRequest(String uri, String body, Map<String, String> headers) {
        Metadata metadata = Metadata.newBuilder()
                .setUri(uri)
                .putAllHeaders(headers)
                .build();

        return GrpcSnailAiRequest.newBuilder()
                .setReqId(REQ_ID_GEN.incrementAndGet())
                .setMetadata(metadata)
                .setBody(body != null ? body : "")
                .build();
    }

    private static MethodDescriptor<GrpcSnailAiRequest, GrpcSnailAiResult> buildMethodDescriptor(
            MethodDescriptor.MethodType type, String serviceName, String methodName) {
        return MethodDescriptor.<GrpcSnailAiRequest, GrpcSnailAiResult>newBuilder()
                .setType(type)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                .setRequestMarshaller(ProtoUtils.marshaller(GrpcSnailAiRequest.getDefaultInstance()))
                .setResponseMarshaller(ProtoUtils.marshaller(GrpcSnailAiResult.getDefaultInstance()))
                .build();
    }
}
