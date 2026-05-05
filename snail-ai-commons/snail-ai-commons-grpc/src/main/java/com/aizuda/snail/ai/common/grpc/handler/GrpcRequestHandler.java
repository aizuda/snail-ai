package com.aizuda.snail.ai.common.grpc.handler;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;

/**
 * gRPC Unary 请求处理器 — 按 URI 路由的策略接口。
 *
 * @author opensnail
 */
public interface GrpcRequestHandler {

    boolean supports(String uri);

    GrpcSnailAiResult handle(GrpcHandlerRequest request);
}
