package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档解析结果。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResult {

    /**
     * 用于 RAG 切片和向量化的最终文本内容。
     */
    private String content;

    /**
     * Docling 清理后的 Markdown 内容；默认解析器通常为空。
     */
    private String markdownContent;

    /**
     * Docling 文档元数据 JSON，最终保存到文档表。
     */
    private String docMetadata;

    /**
     * 文档页数。
     */
    private Integer pageCount;

    /**
     * Docling 结构化元素数量。
     */
    private Integer elementCount;

    /**
     * Docling 表格数量。
     */
    private Integer tableCount;

    /**
     * Docling 图片数量。
     */
    private Integer imageCount;

    /**
     * 解析耗时，单位毫秒。
     */
    private Integer parseTime;

    /**
     * Docling 原始结构化结果，用于流水线保存图片；默认解析器为空。
     */
    private DoclingParseResult doclingResult;

    public static DocumentParseResult ofContent(String content) {
        return DocumentParseResult.builder()
                .content(content)
                .build();
    }
}
