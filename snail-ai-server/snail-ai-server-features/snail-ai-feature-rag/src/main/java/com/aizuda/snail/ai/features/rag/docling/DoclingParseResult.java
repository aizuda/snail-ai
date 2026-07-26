package com.aizuda.snail.ai.features.rag.docling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Docling 解析结果。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoclingParseResult {

    /**
     * Docling 调用和响应解析是否成功。
     */
    private boolean success;

    /**
     * 失败原因，供上层包装成业务异常或日志。
     */
    private String errorMessage;

    /**
     * Docling 原始 Markdown 内容，后续会清理 embedded base64 图片后用于切片。
     */
    private String markdownContent;

    /**
     * 文档来源、文件名等元数据 JSON，最终保存到 sai_rag_document.doc_metadata。
     */
    private String docMetadata;

    /**
     * 文档页数。
     */
    private Integer pageCount;

    /**
     * 标题、段落、表格、图片等解析元素总数。
     */
    private Integer elementCount;

    /**
     * 表格元素数量。
     */
    private Integer tableCount;

    /**
     * 可保存图片数量。
     */
    private Integer imageCount;

    /**
     * 解析耗时，单位毫秒。
     */
    private Integer parseTime;

    /**
     * 按文档阅读顺序展开的结构化元素，用于重建更干净的 Markdown。
     */
    @Builder.Default
    private List<DoclingElement> elements = new ArrayList<>();

    /**
     * Docling 返回的图片数据，保存开关由 parseParams.docling.saveImages 控制。
     */
    @Builder.Default
    private List<DoclingImage> images = new ArrayList<>();

    public static DoclingParseResult fail(String errorMessage) {
        return DoclingParseResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoclingElement {
        /**
         * 元素在文档中的顺序。
         */
        private Integer index;
        /**
         * 元素类型，例如 title、paragraph、table、picture。
         */
        private String type;
        /**
         * 元素文本内容；表格会转换成 Markdown 表格文本。
         */
        private String text;
        /**
         * 元素所在页码。
         */
        private Integer page;
        /**
         * 标题层级，普通段落为 0。
         */
        private Integer level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoclingImage {
        /**
         * 图片 base64 内容，不包含 data:image 前缀。
         */
        private String base64;
        /**
         * 图片说明文字或图注。
         */
        private String caption;

        private String figureNo;

        private String figureTitle;

        private String sectionTitle;

        private Integer elementIndex;

        /**
         * 图片来源页码。
         */
        private Integer page;
        /**
         * 图片 OCR 文本，供前端展示或后续检索扩展使用。
         */
        private String ocrText;
    }
}
