package com.aizuda.snail.ai.model.common;

/**
 * 模型适配异常，携带 {@link ModelErrorCode} 标识具体失败原因。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class ModelAdapterException extends RuntimeException {

    private final ModelErrorCode errorCode;

    public ModelAdapterException(ModelErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ModelAdapterException(ModelErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ModelErrorCode getErrorCode() {
        return errorCode;
    }
}
