package com.aizuda.snail.ai.admin.vo.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 记忆配置新增/更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class MemoryConfigRequestVO {

    /** 配置名称 */
    @NotBlank
    private String name;

    /** 配置状态 */
    private Integer status;

    /** 向量库实例 ID */
    private Long vectorStoreInstanceId;

    /** 向量化（嵌入）模型 ID */
    @NotNull
    private Long embeddingModelId;

    /** 是否启用搜索引擎 */
    private Boolean searchEngineEnable;

    /** 搜索引擎实例 ID */
    private Long searchEngineInstanceId;

    /** 最大召回数量 */
    private Integer maxRecall;

    /** 是否启用查询改写 */
    private Boolean rewriteEnabled;

    /** 是否启用重排序 */
    private Boolean rerankEnabled;

    /** 重排序模型 ID */
    private Long rerankModelId;

    /** 相似度阈值 */
    private BigDecimal similarityThreshold;

    /** 是否启用阈值过滤 */
    private Boolean thresholdEnabled;

    /** 进入重排序的候选数量 */
    private Integer enterRerankCount;

    /** 召回结果融合策略 */
    private String fusionStrategy;

    /** 稠密向量权重 */
    private Double denseWeight;

    /** RRF 融合参数 K */
    private Integer rrfK;

    /** 记忆抽取间隔 */
    private Integer extractionInterval;

    /** 单次抽取最大记忆数 */
    private Integer maxMemoriesPerExtraction;

    /** 记忆抽取模型 ID */
    private Long extractionModelId;

    /** 记忆过期天数 */
    private Integer memoryExpirationDays;

    /** 自定义记忆抽取提示词（为空则使用默认） */
    private String customExtractionPrompt;

    /** 自定义记忆更新提示词 */
    private String customUpdatePrompt;

    /** @deprecated 已废弃，请使用 customExtractionPrompt */
    @Deprecated
    private String extractionRuleType;

    /** @deprecated 已废弃，请使用 customExtractionPrompt */
    @Deprecated
    private String extractionRuleInstruction;

    /** @deprecated 已废弃，请使用 customExtractionPrompt */
    @Deprecated
    private String extractionPromptType;
}
