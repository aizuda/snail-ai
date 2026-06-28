package com.aizuda.snail.ai.common.execption;

import lombok.Getter;

/**
 * (复制自 snail-job)
 */
@Getter
public class SnailAiAuthenticationException extends BaseSnailAiException {
    private final Integer errorCode = 5001;

    public SnailAiAuthenticationException(final String message) {
        super(message);
    }

    public SnailAiAuthenticationException(String message, Object... arguments) {
        super(message, arguments);
    }

    public SnailAiAuthenticationException(String message, Object[] arguments, Throwable cause) {
        super(message, arguments, cause);
    }

    public SnailAiAuthenticationException(String message, Object argument, Throwable cause) {
        super(message, argument, cause);
    }

    public SnailAiAuthenticationException(String message, Object argument) {
        super(message, argument);
    }
}
