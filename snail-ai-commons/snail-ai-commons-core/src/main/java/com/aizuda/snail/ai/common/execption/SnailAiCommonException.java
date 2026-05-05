package com.aizuda.snail.ai.common.execption;

/**
 * 异常信息
 */
public class SnailAiCommonException extends BaseSnailAiException {

    public SnailAiCommonException(String message) {
        super(message);
    }

    public SnailAiCommonException(String message, Object... arguments) {
        super(message, arguments);
    }

    public SnailAiCommonException(String message, Object[] arguments, Throwable cause) {
        super(message, arguments, cause);
    }

    public SnailAiCommonException(String message, Object argument, Throwable cause) {
        super(message, argument, cause);
    }

    public SnailAiCommonException(String message, Object argument) {
        super(message, argument);
    }
}
