package com.aizuda.snail.ai.features.rag.pipeline;

import java.util.List;

/**
 * 图片 OCR 处理器。
 *
 * @author opensnail
 * @since 2026-07-03
 */
public interface ImageOcrHandler {

    /**
     * 识别图片文字。
     *
     * @param request 图片 OCR 请求上下文
     * @return OCR 文本，未识别或失败时返回空字符串
     */
    String recognize(ImageOcrRequest request);

    default List<PaddleImageOcrResult> recognizeBatch(List<ImageOcrRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(request -> PaddleImageOcrResult.success(request, recognize(request)))
                .toList();
    }
}
