package com.aizuda.snail.ai.admin.vo.knowledge;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * 知识库视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KnowledgeResponseVO {

    /** 知识库 ID */
    private Long id;

    /** 知识库名称 */
    private String name;

    /** 知识库描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 向量库存储实例 ID */
    private Long vectorStoreInstanceId;

    /** 向量维度（为空时使用模型默认维度） */
    private Integer dimensionOfVectorModel;

    /** 向量模型 ID */
    private Long embeddingModelId;

    /** 向量模型展示名（由 embeddingModelId 解析，仅响应用） */
    private String embeddingModelName;

    /** 重排模型 ID */
    private Long rerankModelId;

    /** 是否启用搜索引擎 */
    private Boolean searchEngineEnable;

    /** 搜索引擎实例 ID */
    private Long searchEngineInstanceId;

    /** 分隔符 */
    private String delimiter;

    /** RAG 增强策略 */
    @JsonAlias("knowledgeEnhancement")
    private String ragEnhancement;

    /** 知识库配置 */
    private KnowledgeConfigRequestVO config;

    /** 文档数量 */
    private Integer documentCount;

    /** 切片数量 */
    private Integer chunkCount;

    /** 去重策略: 0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT */
    private Integer dedupStrategy;

    /** 冲突动作: 0=REJECT 1=SKIP 2=OVERWRITE */
    private Integer dedupAction;

    /** 上传前是否需要二次确认 */
    private Boolean uploadConfirm;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}
