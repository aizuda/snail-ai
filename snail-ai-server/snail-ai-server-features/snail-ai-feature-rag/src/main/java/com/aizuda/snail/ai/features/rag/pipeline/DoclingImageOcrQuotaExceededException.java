package com.aizuda.snail.ai.features.rag.pipeline;

/**
 * Docling 图片 OCR 模型配额或限流异常。
 *
 * @author opensnail
 * @since 2026-07-03
 */
public class DoclingImageOcrQuotaExceededException extends RuntimeException {

    public DoclingImageOcrQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
