package com.aizuda.snail.ai.agent.common.exception;

/**
 * 回调超时异常（可重试）
 *
 * @author opensnail
 * @date 2025-04-12
 */
public class CallbackTimeoutException extends CallbackException {
    
    public CallbackTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
