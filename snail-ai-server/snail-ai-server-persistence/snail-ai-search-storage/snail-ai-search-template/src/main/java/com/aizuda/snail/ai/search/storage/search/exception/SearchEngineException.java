package com.aizuda.snail.ai.search.storage.search.exception;

/**
 * SearchEngineException
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class SearchEngineException extends RuntimeException {

    public SearchEngineException(String message) {
        super(message);
    }

    public SearchEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
