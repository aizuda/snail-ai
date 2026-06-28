package com.aizuda.snail.ai.agent.common.handler;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.agent.common.counter.ActiveChatCounter;
import com.aizuda.snail.ai.agent.common.rpc.GrpcChannelProvider;
import com.aizuda.snail.ai.common.dto.beat.HeartbeatBodyRequest;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.client.GrpcChannelUtil;
import com.aizuda.snail.ai.common.grpc.constant.HeaderConstants;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.util.JsonUtil;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 客户端心跳注册任务（每 10 秒向 Server 发送心跳）
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Slf4j
public class ClientHeartbeatScheduler {

    /** 心跳间隔（秒） */
    private static final long HEARTBEAT_INTERVAL_SECONDS = 10;
    
    /** gRPC 保活时间（秒） */
    private static final int GRPC_KEEP_ALIVE_TIME_SECONDS = 30;
    
    /** gRPC 保活超时（秒） */
    private static final int GRPC_KEEP_ALIVE_TIMEOUT_SECONDS = 10;
    
    /** gRPC 空闲超时（分钟） */
    private static final int GRPC_IDLE_TIMEOUT_MINUTES = 5;
    
    /** 主机ID长度 */
    private static final int HOST_ID_LENGTH = 16;
    
    /** 默认主机IP */
    private static final String DEFAULT_HOST_IP = "127.0.0.1";
    
    /** 客户端版本 */
    private static final String CLIENT_VERSION = "1.0.0";
    
    /** 心跳状态：成功 */
    private static final int HEARTBEAT_STATUS_SUCCESS = 1;

    private final SnailAiAgentProperties properties;
    private final ActiveChatCounter activeChatCounter;
    private final GrpcChannelProvider grpcChannelProvider;
    private final String hostId;
    private final String hostIp;

    @Getter
    private ManagedChannel serverChannel;
    private ScheduledExecutorService scheduler;

    public ClientHeartbeatScheduler(SnailAiAgentProperties properties,
                                    ActiveChatCounter activeChatCounter,
                                    GrpcChannelProvider grpcChannelProvider) {
        this.properties = properties;
        this.activeChatCounter = activeChatCounter;
        this.grpcChannelProvider = grpcChannelProvider;
        this.hostId = generateHostId();
        this.hostIp = resolveLocalIp();
    }

    public void start() {
        serverChannel = createServerChannel();
        
        // 更新 GrpcChannelProvider
        grpcChannelProvider.updateChannel(serverChannel, buildHeartbeatHeaders());
        
        scheduler = createScheduler();
        scheduleHeartbeat();
        
        log.info("Heartbeat started: appId={}, server={}:{}", 
                properties.getAppId(),  properties.getServer().getHost(), properties.getServer().getPort());
    }

    public void stop() {
        shutdownScheduler();
        shutdownChannel();
    }

    private ManagedChannel createServerChannel() {
        return NettyChannelBuilder
                .forAddress(properties.getServer().getHost(), properties.getServer().getPort())
                .usePlaintext()
                .keepAliveTime(GRPC_KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS)
                .keepAliveTimeout(GRPC_KEEP_ALIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .idleTimeout(GRPC_IDLE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    private ScheduledExecutorService createScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snail-ai-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    private void scheduleHeartbeat() {
        scheduler.scheduleAtFixedRate(
                this::sendHeartbeat, 
                0, 
                HEARTBEAT_INTERVAL_SECONDS, 
                TimeUnit.SECONDS);
    }

    private void shutdownScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void shutdownChannel() {
        if (serverChannel != null && !serverChannel.isShutdown()) {
            serverChannel.shutdown();
        }
    }

    private void sendHeartbeat() {
        try {
            Map<String, String> headers = buildHeartbeatHeaders();
            String body = buildHeartbeatBody();

            GrpcSnailAiResult result = GrpcChannelUtil.sendUnary(
                    serverChannel, UriConstants.BEAT, body, headers);

            if (result.getStatus() != HEARTBEAT_STATUS_SUCCESS) {
                log.warn("Heartbeat rejected: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.warn("Heartbeat failed: {}, attempting reconnect", e.getMessage());
            attemptReconnect();
        }
    }

    /**
     * 尝试重新连接到服务器
     */
    private void attemptReconnect() {
        try {
            log.info("Attempting to reconnect to server");
            
            // 关闭旧 channel
            if (serverChannel != null && !serverChannel.isShutdown()) {
                serverChannel.shutdown();
            }
            
            // 创建新 channel
            serverChannel = createServerChannel();
            
            // 更新 GrpcChannelProvider
            grpcChannelProvider.updateChannel(serverChannel, buildHeartbeatHeaders());
            
            log.info("Reconnected to server successfully");
        } catch (Exception e) {
            log.error("Failed to reconnect to server", e);
        }
    }

    public Map<String, String> buildHeartbeatHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HeaderConstants.HOST_ID, hostId);
        headers.put(HeaderConstants.HOST_IP, hostIp);
        headers.put(HeaderConstants.HOST_PORT, String.valueOf(properties.getPort()));
        headers.put(HeaderConstants.APP_ID, properties.getAppId());
        headers.put(HeaderConstants.TOKEN, properties.getToken());
        headers.put(HeaderConstants.VERSION, CLIENT_VERSION);
        return headers;
    }

    private String buildHeartbeatBody() {
        HeartbeatBodyRequest body = new HeartbeatBodyRequest(
                properties.getMaxConcurrentChats(),
                activeChatCounter.get());
        return JsonUtil.toJsonString(body);
    }

    private String generateHostId() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, HOST_ID_LENGTH);
    }

    private String resolveLocalIp() {
        try {
            String host = properties.getHost();
            if (StrUtil.isNotBlank(host)) {
                return host;
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Failed to resolve local IP, using default: {}", DEFAULT_HOST_IP);
            return DEFAULT_HOST_IP;
        }
    }
}
