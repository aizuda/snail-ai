package com.aizuda.snail.ai.common.execption;

/**
 * (复制自 snail-job-ai-executor)
 */
public class SnailAiException extends BaseSnailAiException {

    public SnailAiException(String message) {
        super(message);
    }

    public SnailAiException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnailAiException(Throwable cause) {
        super(cause);
    }

    public SnailAiException(String message, Object... arguments) {
        super(message, arguments);
    }

    public SnailAiException(String message, Object[] arguments, Throwable cause) {
        super(message, arguments, cause);
    }

    public SnailAiException(String message, Object argument, Throwable cause) {
        super(message, argument, cause);
    }

    public SnailAiException(String message, Object argument) {
        super(message, argument);
    }
}
