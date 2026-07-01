package com.aizuda.snail.ai.features.rag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档解析引擎。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Getter
@AllArgsConstructor
public enum DocumentParseEngineEnum {

    /**
     * 项目内置解析器，保持原有 PDFBox/POI/纯文本解析链路。
     */
    DEFAULT("default"),

    /**
     * 外部 Docling Serve 解析器，用于结构化解析、表格识别、图片提取和 OCR。
     */
    DOCLING("docling");

    /**
     * 持久化到知识库配置中的解析引擎标识。
     */
    private final String value;

    public static DocumentParseEngineEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        for (DocumentParseEngineEnum engine : values()) {
            if (engine.value.equalsIgnoreCase(value.trim())) {
                return engine;
            }
        }
        return DEFAULT;
    }
}
