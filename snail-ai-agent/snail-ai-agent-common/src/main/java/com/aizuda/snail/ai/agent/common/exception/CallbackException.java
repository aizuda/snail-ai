package com.aizuda.snail.ai.agent.common.exception;

import com.aizuda.snail.ai.common.execption.SnailAiException;

/**
 * Server 回调基础异常
 *
 * @author opensnail
 * @date 2025-04-12
 */
public class CallbackException extends SnailAiException {
    
    public CallbackException(String message) {
        super(message);
    }
    
    public CallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
