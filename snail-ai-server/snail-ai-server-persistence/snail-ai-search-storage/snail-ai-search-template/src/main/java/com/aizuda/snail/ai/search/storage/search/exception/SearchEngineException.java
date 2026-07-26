package com.aizuda.snail.ai.search.storage.search.exception;

/**
 * 全文检索引擎异常，封装搜索引擎初始化、读写过程中的错误。
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
