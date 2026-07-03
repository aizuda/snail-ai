package com.aizuda.snail.ai.admin.vo.knowledge;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库新增/更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class KnowledgeRequestVO {

    /** 知识库名称 */
    @NotBlank(message = "name is required")
    private String name;

    /** 知识库描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 向量库存储实例 ID（VECTOR_STORE） */
    private Long vectorStoreInstanceId;

    /** 向量维度(必填)，不能超过模型与向量库最大支持维度 */
    @NotNull(message = "dimensionOfVectorModel is required")
    private Integer dimensionOfVectorModel;

    /** 向量模型 ID */
    @NotNull(message = "embeddingModelId is required")
    private Long embeddingModelId;

    /** 重排模型 ID */
    private Long rerankModelId;

    /** 是否启用搜索引擎 */
    private Boolean searchEngineEnable;

    /** 搜索引擎实例 ID（SEARCH_ENGINE），混合检索开启时选用 */
    private Long searchEngineInstanceId;

    /** 分隔符 */
    private String delimiter;

    /** RAG 增强策略 */
    @JsonAlias("knowledgeEnhancement")
    private String ragEnhancement;

    // ---------- 切片策略（写入 config.chunkParams，并同步 delimiter 字段）----------

    /** default | delimiter */
    private String chunkMode;

    /** 单片段最大 token 数 */
    private Integer maxChunkTokens;

    /** 切片重叠 token 数 */
    private Integer chunkOverlap;

    /** chunkMode=delimiter 时的一级分隔符 */
    private String customDelimiter;

    /** chunkMode=regex 时的一级切分正则（Java Pattern 语法） */
    private String chunkRegex;

    /** chunkMode=smart 时用于智能切片的对话模型 ID（模型配置） */
    private Long chunkModelId;

    /** 是否合并过短片段 */
    private Boolean mergeShortSegments;

    /** 是否开启图片 OCR */
    private Boolean imageOcr;

    // ---------- 上传去重策略 ----------

    /** 去重策略: 0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT */
    private Integer dedupStrategy;

    /** 冲突动作: 0=REJECT 1=SKIP 2=OVERWRITE */
    private Integer dedupAction;

    /** 上传前是否需要二次确认 */
    private Boolean uploadConfirm;

    // ---------- 解析引擎 ----------

    /** 解析引擎配置：default | docling */
    private String parseEngine;
    /** Docling OCR 开关 */
    private Boolean doclingDoOcr;
    /** Docling 表格识别开关 */
    private Boolean doclingDoTableStructure;
    /** Docling 保存图片开关 */
    private Boolean doclingSaveImages;
    /** 解析引擎配置对象（前端可能直接传 JSON 对象） */
    private KnowledgeConfigRequestVO.ParseParams parseParams;
}
