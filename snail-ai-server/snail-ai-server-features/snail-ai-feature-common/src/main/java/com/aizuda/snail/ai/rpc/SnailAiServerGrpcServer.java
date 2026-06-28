package com.aizuda.snail.ai.rpc;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestDispatcher;
import com.aizuda.snail.ai.common.grpc.server.GrpcServiceDefinitionBuilder;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Server 端 gRPC 服务器（接收客户端心跳和结果上报）
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnailAiServerGrpcServer implements SmartLifecycle {

    /** gRPC 最大入站消息大小（字节） */
    private static final int GRPC_MAX_INBOUND_MESSAGE_SIZE = 10 * 1024 * 1024;

    /** gRPC 保活时间（秒） */
    private static final int GRPC_KEEP_ALIVE_TIME_SECONDS = 30;

    /** gRPC 保活超时（秒） */
    private static final int GRPC_KEEP_ALIVE_TIMEOUT_SECONDS = 10;

    /** gRPC 允许保活时间（分钟） */
    private static final int GRPC_PERMIT_KEEP_ALIVE_MINUTES = 5;

    /** 关闭等待时间（秒） */
    private static final int SHUTDOWN_AWAIT_SECONDS = 10;

    /** 生命周期阶段 */
    private static final int LIFECYCLE_PHASE = Integer.MAX_VALUE - 100;

    private final GrpcRequestDispatcher grpcRequestDispatcher;

    @Value("${snail-ai.server.grpc-port:1789}")
    private int grpcPort;

    private Server server;
    private volatile boolean running = false;

    @Override
    public void start() {
        try {
            server = buildGrpcServer();
            server.start();
            running = true;
            log.info("Snail-AI Server gRPC started on port {}", grpcPort);
        } catch (Exception e) {
            log.error("Failed to start Server gRPC on port {}", grpcPort, e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        running = false;
        log.info("Snail-AI Server gRPC stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LIFECYCLE_PHASE;
    }

    private Server buildGrpcServer() {
        return NettyServerBuilder.forPort(grpcPort)
                .addService(GrpcServiceDefinitionBuilder.createUnaryServiceDefinition(
                        this::handleUnaryRequest))
                .maxInboundMessageSize(GRPC_MAX_INBOUND_MESSAGE_SIZE)
                .keepAliveTime(GRPC_KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS)
                .keepAliveTimeout(GRPC_KEEP_ALIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .permitKeepAliveTime(GRPC_PERMIT_KEEP_ALIVE_MINUTES, TimeUnit.MINUTES)
                .permitKeepAliveWithoutCalls(true)
                .build();
    }

    private void handleUnaryRequest(GrpcSnailAiRequest request, StreamObserver<GrpcSnailAiResult> observer) {
        String uri = request.getMetadata().getUri();
        Map<String, String> headers = request.getMetadata().getHeadersMap();

        try {
            GrpcSnailAiResult result = grpcRequestDispatcher.dispatchUnary(
                    request.getReqId(), uri, headers, request.getBody());
            observer.onNext(result);
            observer.onCompleted();
        } catch (Exception e) {
            log.error("Error handling gRPC request uri={}", uri, e);
            observer.onNext(buildErrorResult(request.getReqId(), e.getMessage()));
            observer.onCompleted();
        }
    }

    private GrpcSnailAiResult buildErrorResult(long reqId, String errorMessage) {
        return GrpcSnailAiResult.newBuilder()
                .setReqId(reqId)
                .setStatus(0)
                .setMessage(errorMessage != null ? errorMessage : "Unknown error")
                .build();
    }
}
