package com.aizuda.snail.ai.common.grpc.constant;

/**
 * gRPC 服务常量
 */
public interface GrpcConstants {

    /** Unary 服务（心跳、结果上报等） */
    String UNARY_SERVICE_NAME = "UnaryRequest";
    String UNARY_METHOD_NAME = "unaryRequest";

    /** Server Streaming 服务（Chat 分发，流式返回 chunks） */
    String STREAMING_SERVICE_NAME = "ServerStreamingRequest";
    String STREAMING_METHOD_NAME = "serverStreamingRequest";

    /** 默认 Server gRPC 端口 */
    int DEFAULT_SERVER_GRPC_PORT = 1789;

    /** 默认 Client gRPC 端口 */
    int DEFAULT_CLIENT_GRPC_PORT = 1790;
}
