package com.aizuda.snail.ai.model.common;

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
