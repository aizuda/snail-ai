package com.aizuda.snail.ai.agent.common.rpc;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.server.GrpcServiceDefinitionBuilder;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 客户端 gRPC 服务器（接收 Server 的 dispatch 请求）
 * <p>
 * 注册两种服务：Unary（Ping）+ Server Streaming（DispatchChat）
 */
@Slf4j
public class ClientGrpcServer {

    private Server server;
    private final int port;
    private final ServerCalls.UnaryMethod<GrpcSnailAiRequest, GrpcSnailAiResult> unaryHandler;
    private final ServerCalls.ServerStreamingMethod<GrpcSnailAiRequest, GrpcSnailAiResult> streamingHandler;

    public ClientGrpcServer(int port,
                            ServerCalls.UnaryMethod<GrpcSnailAiRequest, GrpcSnailAiResult> unaryHandler,
                            ServerCalls.ServerStreamingMethod<GrpcSnailAiRequest, GrpcSnailAiResult> streamingHandler) {
        this.port = port;
        this.unaryHandler = unaryHandler;
        this.streamingHandler = streamingHandler;
    }

    public void start() throws Exception {
        server = NettyServerBuilder.forPort(port)
                .addService(GrpcServiceDefinitionBuilder.createUnaryServiceDefinition(unaryHandler))
                .addService(GrpcServiceDefinitionBuilder.createServerStreamingServiceDefinition(streamingHandler))
                .maxInboundMessageSize(10 * 1024 * 1024)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .permitKeepAliveTime(30, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .build()
                .start();

        log.info("Client gRPC server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Client gRPC server stopped");
    }
}
