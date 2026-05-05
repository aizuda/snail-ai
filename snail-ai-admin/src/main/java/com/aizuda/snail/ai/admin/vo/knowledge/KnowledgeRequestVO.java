package com.aizuda.snail.ai.admin.vo.knowledge;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeRequestVO {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private String icon;

    /** 向量库存储实例 ID（VECTOR_STORE） */
    private Long vectorStoreInstanceId;

    @NotNull(message = "embeddingModelId is required")
    private Long embeddingModelId;

    private Long rerankModelId;

    private Boolean searchEngineEnable;

    /** 搜索引擎实例 ID（SEARCH_ENGINE），混合检索开启时选用 */
    private Long searchEngineInstanceId;

    private String delimiter;

    @JsonAlias("knowledgeEnhancement")
    private String ragEnhancement;

    // ---------- 切片策略（写入 config.chunkParams，并同步 delimiter 字段）----------

    /** default | delimiter */
    private String chunkMode;

    private Integer maxChunkTokens;

    private Integer chunkOverlap;

    /** chunkMode=delimiter 时的一级分隔符 */
    private String customDelimiter;

    /** chunkMode=regex 时的一级切分正则（Java Pattern 语法） */
    private String chunkRegex;

    /** chunkMode=smart 时用于智能切片的对话模型 ID（模型配置） */
    private Long chunkModelId;

    private Boolean mergeShortSegments;

    private Boolean imageOcr;

    // ---------- 上传去重策略 ----------

    /** 去重策略: 0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT */
    private Integer dedupStrategy;

    /** 冲突动作: 0=REJECT 1=SKIP 2=OVERWRITE */
    private Integer dedupAction;

    /** 上传前是否需要二次确认 */
    private Boolean uploadConfirm;
}
