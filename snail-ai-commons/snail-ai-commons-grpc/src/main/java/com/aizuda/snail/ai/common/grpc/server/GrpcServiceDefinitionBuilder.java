package com.aizuda.snail.ai.common.grpc.server;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.GrpcConstants;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 服务定义构建工具（与 snail-job GrpcServer 模式一致）
 * <p>
 * 提供 Unary 和 Server Streaming 两种服务定义的编程式构建。
 */
@Slf4j
public final class GrpcServiceDefinitionBuilder {

    private GrpcServiceDefinitionBuilder() {}

    /**
     * 创建 Unary 服务定义
     */
    public static ServerServiceDefinition createUnaryServiceDefinition(
            ServerCalls.UnaryMethod<GrpcSnailAiRequest, GrpcSnailAiResult> unaryMethod) {
        return createUnaryServiceDefinition(
                GrpcConstants.UNARY_SERVICE_NAME,
                GrpcConstants.UNARY_METHOD_NAME,
                unaryMethod);
    }

    public static ServerServiceDefinition createUnaryServiceDefinition(
            String serviceName, String methodName,
            ServerCalls.UnaryMethod<GrpcSnailAiRequest, GrpcSnailAiResult> unaryMethod) {

        MethodDescriptor<GrpcSnailAiRequest, GrpcSnailAiResult> methodDescriptor =
                MethodDescriptor.<GrpcSnailAiRequest, GrpcSnailAiResult>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                        .setRequestMarshaller(ProtoUtils.marshaller(GrpcSnailAiRequest.getDefaultInstance()))
                        .setResponseMarshaller(ProtoUtils.marshaller(GrpcSnailAiResult.getDefaultInstance()))
                        .build();

        return ServerServiceDefinition.builder(serviceName)
                .addMethod(methodDescriptor, ServerCalls.asyncUnaryCall(unaryMethod))
                .build();
    }

    /**
     * 创建 Server Streaming 服务定义
     */
    public static ServerServiceDefinition createServerStreamingServiceDefinition(
            ServerCalls.ServerStreamingMethod<GrpcSnailAiRequest, GrpcSnailAiResult> streamingMethod) {
        return createServerStreamingServiceDefinition(
                GrpcConstants.STREAMING_SERVICE_NAME,
                GrpcConstants.STREAMING_METHOD_NAME,
                streamingMethod);
    }

    public static ServerServiceDefinition createServerStreamingServiceDefinition(
            String serviceName, String methodName,
            ServerCalls.ServerStreamingMethod<GrpcSnailAiRequest, GrpcSnailAiResult> streamingMethod) {

        MethodDescriptor<GrpcSnailAiRequest, GrpcSnailAiResult> methodDescriptor =
                MethodDescriptor.<GrpcSnailAiRequest, GrpcSnailAiResult>newBuilder()
                        .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
                        .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                        .setRequestMarshaller(ProtoUtils.marshaller(GrpcSnailAiRequest.getDefaultInstance()))
                        .setResponseMarshaller(ProtoUtils.marshaller(GrpcSnailAiResult.getDefaultInstance()))
                        .build();

        return ServerServiceDefinition.builder(serviceName)
                .addMethod(methodDescriptor, ServerCalls.asyncServerStreamingCall(streamingMethod))
                .build();
    }
}
