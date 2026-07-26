package com.aizuda.snail.ai.persistence.rag.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * author: zhangshuguang
 * date: 2026-04-01
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagConfigDO {

    private SearchParams searchParams;

    private ModelParams modelParams;

    /** 文档切片策略（创建知识库时写入） */
    private ChunkParams chunkParams;

    /** 文档解析引擎配置；为空时使用项目内置默认解析器。 */
    private ParseParams parseParams;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseParams {
        /** 解析引擎：default 使用内置解析器，docling 使用外部 Docling Serve。 */
        private String engine;
        /** Docling 专属参数，仅 engine=docling 时生效。 */
        private DoclingParams docling;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoclingParams {
        /** 是否启用 OCR，适用于扫描件或图片型 PDF。 */
        private Boolean doOcr;
        /** 是否启用表格结构识别。 */
        private Boolean doTableStructure;
        /** 图片导出模式，默认 embedded。 */
        private String imageExportMode;
        /** OCR 语言列表，例如 en、zh。 */
        private List<String> ocrLang;
        /** PDF 解析后端，随 Docling 服务版本调整。 */
        private String pdfBackend;
        /** 是否保存 Docling 提取出的图片到资源库。 */
        private Boolean saveImages;
        /** 单个文档最多保存的 Docling 图片数；为空时使用全局 snail-ai.rag.docling.max-image-count。 */
        private Integer maxImageCount;
        /** 单张 Docling 图片最大字节数；为空时使用全局 snail-ai.rag.docling.max-image-bytes。 */
        private Long maxImageBytes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkParams {
        /** length | delimiter | regex | smart */
        private String mode;
        /** 单片段最大 token/字符量级（与 recursive splitter 一致） */
        private Integer maxChunkTokens;
        private Integer chunkOverlap;
        /** mode=delimiter 时作为一级切分符 */
        private String customDelimiter;
        /** mode=regex 时的一级切分正则（Java Pattern） */
        private String chunkRegex;
        /** mode=smart 时用于语义切片的对话模型配置 ID */
        private Long chunkModelId;
        private Boolean mergeShortSegments;
        private Boolean imageOcr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchParams {
        private Integer resultCount;
        private Boolean rerankEnabled;
        private Long rerankModelId;
        private Integer enterRerankCount;
        private Boolean thresholdEnabled;
        private Double threshold;
        private Double denseWeight;
        private String fusionStrategy;
        private Integer rrfK;
        private Boolean questionRewrite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelParams {
        private Long modelId;
        private Integer nearbySliceCount;
        private String prompt;
    }
}
