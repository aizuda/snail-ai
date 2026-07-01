package com.aizuda.snail.ai.features.rag.docling;

import com.aizuda.snail.ai.common.execption.SnailAiException;

/**
 * Docling 解析异常。
 *
 * @author opensnail
 * @since 2026-06-30
 */
public class DoclingException extends SnailAiException {

    public DoclingException(String message) {
        super(message);
    }

    public DoclingException(String message, Throwable cause) {
        super(message, cause);
    }
}
