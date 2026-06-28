package com.aizuda.snail.ai.agent.common.rpc;

import com.aizuda.snail.ai.agent.common.exception.CallbackChannelUnavailableException;
import io.grpc.ManagedChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * gRPC Channel 生命周期管理
 * <p>
 * 负责管理与 Server 的 gRPC 连接和请求头，支持运行时更新
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Component
@Slf4j
public class GrpcChannelProvider {
    
    private volatile ManagedChannel channel;
    /**
     * -- GETTER --
     *  获取请求头
     */
    @Getter
    private volatile Map<String, String> headers = Map.of();
    
    /**
     * 更新 channel 和请求头（由 heartbeat 任务调用）
     */
    public void updateChannel(ManagedChannel channel, Map<String, String> headers) {
        ManagedChannel oldChannel = this.channel;
        this.channel = channel;
        this.headers = headers;
        
        log.info("gRPC channel updated, state: {}", 
            channel != null ? channel.getState(false) : "null");
        
        // 优雅关闭旧 channel
        if (oldChannel != null && oldChannel != channel && !oldChannel.isShutdown()) {
            try {
                oldChannel.shutdown();
                log.debug("Old channel shutdown");
            } catch (Exception e) {
                log.warn("Failed to shutdown old channel", e);
            }
        }
    }
    
    /**
     * 获取当前可用的 channel
     * 
     * @throws CallbackChannelUnavailableException 如果 channel 不可用
     */
    public ManagedChannel getChannel() {
        ManagedChannel ch = this.channel;
        if (ch == null || ch.isShutdown() || ch.isTerminated()) {
            throw new CallbackChannelUnavailableException("gRPC channel is unavailable");
        }
        return ch;
    }

}
