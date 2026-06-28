package com.aizuda.snail.ai.agent.common.exception;

/**
 * gRPC Channel 不可用异常（可重试）
 *
 * @author opensnail
 * @date 2025-04-12
 */
public class CallbackChannelUnavailableException extends CallbackException {
    
    public CallbackChannelUnavailableException(String message) {
        super(message);
    }
}
