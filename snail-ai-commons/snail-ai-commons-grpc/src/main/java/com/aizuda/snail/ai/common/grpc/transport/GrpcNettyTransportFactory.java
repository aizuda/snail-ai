package com.aizuda.snail.ai.common.grpc.transport;

import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

/**
 * gRPC Netty 传输层 Builder 工厂。
 *
 * <p>JDK 25 开始会对 {@code sun.misc.Unsafe} 内存分配输出终止弃用告警。shaded Netty
 * 仍可使用标准 NIO 路径，因此在 JDK 25 及以上默认禁用 Unsafe；显式设置系统属性时保留调用方配置。
 *
 * @author opensnail
 * @since 1.1.1
 */
public final class GrpcNettyTransportFactory {

    static final String NETTY_NO_UNSAFE_PROPERTY = "io.grpc.netty.shaded.io.netty.noUnsafe";
    private static final int UNSAFE_TERMINAL_DEPRECATION_JAVA_VERSION = 25;

    private GrpcNettyTransportFactory() {
    }

    public static NettyServerBuilder forPort(int port) {
        configureUnsafePolicy();
        return NettyServerBuilder.forPort(port);
    }

    public static NettyChannelBuilder forAddress(String host, int port) {
        configureUnsafePolicy();
        return NettyChannelBuilder.forAddress(host, port);
    }

    static void configureUnsafePolicy() {
        if (Runtime.version().feature() < UNSAFE_TERMINAL_DEPRECATION_JAVA_VERSION) {
            return;
        }
        if (System.getProperty(NETTY_NO_UNSAFE_PROPERTY) != null) {
            return;
        }
        System.setProperty(NETTY_NO_UNSAFE_PROPERTY, Boolean.TRUE.toString());
    }
}
