package com.aizuda.snail.ai.agent.common.exception;

/**
 * Server 端业务错误异常（不可重试）
 *
 * @author opensnail
 * @date 2025-04-12
 */
public class CallbackServerErrorException extends CallbackException {
    
    public CallbackServerErrorException(String message) {
        super(message);
    }
}
