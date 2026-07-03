package com.aizuda.snail.ai.features.rag.pipeline;

import lombok.Builder;
import lombok.Data;

/**
 * 图片 OCR 请求上下文。
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Data
@Builder
public class ImageOcrRequest {

    /**
     * 文档 ID，用于日志定位。
     */
    private Long documentId;

    /**
     * 图片在文档中的序号，用于日志定位。
     */
    private Integer imageIndex;

    /**
     * 图片 base64 内容，不包含 data:image 前缀。
     */
    private String base64Image;

    /**
     * 视觉模型配置 ID；仅模型兜底 OCR 使用。
     */
    private Long modelConfigId;
}
