package com.aizuda.snail.ai.admin.vo.knowledge;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KnowledgeResponseVO {

    private Long id;

    private String name;

    private String description;

    private String icon;

    private Long vectorStoreInstanceId;

    /** 向量维度（为空时使用模型默认维度） */
    private Integer dimensionOfVectorModel;

    private Long embeddingModelId;

    /** 向量模型展示名（由 embeddingModelId 解析，仅响应用） */
    private String embeddingModelName;

    private Long rerankModelId;

    private Boolean searchEngineEnable;

    private Long searchEngineInstanceId;

    private String delimiter;

    @JsonAlias("knowledgeEnhancement")
    private String ragEnhancement;

    private KnowledgeConfigRequestVO config;

    private Integer documentCount;

    private Integer chunkCount;

    /** 去重策略: 0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT */
    private Integer dedupStrategy;

    /** 冲突动作: 0=REJECT 1=SKIP 2=OVERWRITE */
    private Integer dedupAction;

    /** 上传前是否需要二次确认 */
    private Boolean uploadConfirm;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
