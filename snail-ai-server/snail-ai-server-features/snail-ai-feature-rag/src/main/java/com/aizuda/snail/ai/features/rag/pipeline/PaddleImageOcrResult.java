package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * PaddleOCR result for one document image.
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Getter
public class PaddleImageOcrResult {

    public enum Status {
        SUCCESS,
        EMPTY,
        FAILED
    }

    private final Integer imageIndex;
    private final String text;
    private final Status status;
    private final String error;

    private PaddleImageOcrResult(Integer imageIndex, String text, Status status, String error) {
        this.imageIndex = imageIndex;
        this.text = StrUtil.nullToEmpty(text);
        this.status = status;
        this.error = StrUtil.nullToEmpty(error);
    }

    public static PaddleImageOcrResult success(ImageOcrRequest request, String text) {
        if (StrUtil.isBlank(text)) {
            return empty(request);
        }
        return new PaddleImageOcrResult(resolveImageIndex(request), text, Status.SUCCESS, "");
    }

    public static PaddleImageOcrResult empty(ImageOcrRequest request) {
        return new PaddleImageOcrResult(resolveImageIndex(request), "", Status.EMPTY, "");
    }

    public static PaddleImageOcrResult failed(ImageOcrRequest request, String error) {
        return new PaddleImageOcrResult(resolveImageIndex(request), "", Status.FAILED, error);
    }

    private static Integer resolveImageIndex(ImageOcrRequest request) {
        return request == null ? null : request.getImageIndex();
    }
}
