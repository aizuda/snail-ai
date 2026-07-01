package com.aizuda.snail.ai.admin.vo.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库配置请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeConfigRequestVO {

    /** 检索参数 */
    private SearchParams searchParams;

    /** 模型参数 */
    private ModelParams modelParams;

    /** 文档切片策略（创建知识库时写入） */
    private ChunkParams chunkParams;

    /** 文档解析引擎配置；为空时保留数据库中的原配置或使用默认解析器。 */
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
        /** 单个文档最多保存的 Docling 图片数；为空时使用系统全局 Docling 配置。 */
        private Integer maxImageCount;
        /** 单张 Docling 图片最大字节数；为空时使用系统全局 Docling 配置。 */
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
        /** 切片重叠量 */
        private Integer chunkOverlap;
        /** mode=delimiter 时作为一级切分符 */
        private String customDelimiter;
        /** mode=regex 时的一级切分正则（Java Pattern） */
        private String chunkRegex;
        /** mode=smart 时用于语义切片的对话模型配置 ID */
        private Long chunkModelId;
        /** 是否合并过短片段 */
        private Boolean mergeShortSegments;
        /** 是否开启图片 OCR */
        private Boolean imageOcr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchParams {
        /** 返回结果数量 */
        private Integer resultCount;
        /** 是否启用重排 */
        private Boolean rerankEnabled;
        /** 重排模型 ID */
        private Long rerankModelId;
        /** 进入重排的候选数量 */
        private Integer enterRerankCount;
        /** 是否启用相似度阈值 */
        private Boolean thresholdEnabled;
        /** 相似度阈值 */
        private Double threshold;
        /** 稠密向量权重 */
        private Double denseWeight;
        /** 融合策略 */
        private String fusionStrategy;
        /** RRF 融合参数 K */
        private Integer rrfK;
        /** 是否启用问题改写（原 QaParams 独有字段） */
        private Boolean questionRewrite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelParams {
        /** 对话模型 ID */
        private Long modelId;
        /** 邻近切片数量 */
        private Integer nearbySliceCount;
        /** 提示词 */
        private String prompt;
    }
}
