package com.aizuda.snail.ai.vector.storage.exception;

import com.aizuda.snail.ai.common.execption.BaseSnailAiException;

/**
 * VectorStoreException
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class VectorStoreException extends BaseSnailAiException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
