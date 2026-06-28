package com.aizuda.snail.ai.admin.vo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI模型列表响应 VO
 * 用于返回分组/过滤后的模型列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelListResponseVO {

    /**
     * 模型类型 (CHAT/EMBEDDING/RERANKER/IMAGE/SPEECH)
     */
    private String modelType;

    /**
     * 该模型类型下的默认模型ID
     */
    private Long defaultModelId;

    /**
     * 该模型类型下的模型列表
     */
    private List<AiModelConfigVO> models;

    /**
     * 该类型下的总数量
     */
    private Integer total;
}
