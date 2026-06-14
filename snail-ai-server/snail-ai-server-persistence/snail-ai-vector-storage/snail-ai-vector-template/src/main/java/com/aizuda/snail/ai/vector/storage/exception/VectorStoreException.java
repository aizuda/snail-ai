package com.aizuda.snail.ai.vector.storage.exception;

import com.aizuda.snail.ai.common.execption.BaseSnailAiException;

public class VectorStoreException extends BaseSnailAiException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
