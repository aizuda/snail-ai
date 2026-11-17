package com.aizuda.snail.ai.common.grpc.transport;

import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.client.GrpcChannelUtil;
import com.aizuda.snail.ai.common.grpc.server.GrpcServiceDefinitionBuilder;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.netty.util.internal.PlatformDependent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GrpcNettyTransportFactoryTest {

    private static final String LOOPBACK_HOST = "127.0.0.1";
    private static final String TEST_URI = "/test/unsafe-disabled";
    private static final String RESPONSE_MESSAGE = "ok";
    private static final int SUCCESS_STATUS = 1;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int UNSAFE_TERMINAL_DEPRECATION_JAVA_VERSION = 25;

    private String originalNoUnsafeProperty;

    @BeforeEach
    void saveNoUnsafeProperty() {
        originalNoUnsafeProperty = System.getProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY);
    }

    @AfterEach
    void restoreNoUnsafeProperty() {
        if (originalNoUnsafeProperty == null) {
            System.clearProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY);
            return;
        }
        System.setProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY, originalNoUnsafeProperty);
    }

    @Test
    @Order(1)
    void shouldExchangeUnaryCallWithUnsafeDisabled() throws Exception {
        System.clearProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY);
        if (Runtime.version().feature() < UNSAFE_TERMINAL_DEPRECATION_JAVA_VERSION) {
            System.setProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY, Boolean.TRUE.toString());
        }

        Server server = GrpcNettyTransportFactory.forPort(0)
                .addService(GrpcServiceDefinitionBuilder.createUnaryServiceDefinition((request, observer) -> {
                    observer.onNext(GrpcSnailAiResult.newBuilder()
                            .setReqId(request.getReqId())
                            .setStatus(SUCCESS_STATUS)
                            .setMessage(RESPONSE_MESSAGE)
                            .build());
                    observer.onCompleted();
                }))
                .build()
                .start();
        ManagedChannel channel = GrpcNettyTransportFactory.forAddress(LOOPBACK_HOST, server.getPort())
                .usePlaintext()
                .build();

        try {
            GrpcSnailAiResult result = GrpcChannelUtil.sendUnary(channel, TEST_URI, "", Map.of());

            assertEquals(Boolean.TRUE.toString(),
                    System.getProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY));
            assertFalse(PlatformDependent.hasUnsafe());
            assertEquals(SUCCESS_STATUS, result.getStatus());
            assertEquals(RESPONSE_MESSAGE, result.getMessage());
        } finally {
            channel.shutdownNow().awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(2)
    void shouldConfigureUnsafePolicyForCurrentRuntime() {
        System.clearProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY);

        GrpcNettyTransportFactory.configureUnsafePolicy();

        if (Runtime.version().feature() >= UNSAFE_TERMINAL_DEPRECATION_JAVA_VERSION) {
            assertEquals(Boolean.TRUE.toString(),
                    System.getProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY));
            return;
        }
        assertNull(System.getProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY));
    }

    @Test
    @Order(3)
    void shouldPreserveExplicitUnsafePolicy() {
        System.setProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY, Boolean.FALSE.toString());

        GrpcNettyTransportFactory.configureUnsafePolicy();

        assertEquals(Boolean.FALSE.toString(),
                System.getProperty(GrpcNettyTransportFactory.NETTY_NO_UNSAFE_PROPERTY));
    }
}
