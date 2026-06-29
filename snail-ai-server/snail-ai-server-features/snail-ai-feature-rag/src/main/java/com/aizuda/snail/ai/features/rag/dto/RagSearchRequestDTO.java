package com.aizuda.snail.ai.features.rag.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RAG 检索请求：知识库 ID、查询语句与调试开关。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagSearchRequestDTO {

    /**
     * 知识库 ID（兼容别名 knowledgeId）
     */
    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    /**
     * 查询语句
     */
    @NotBlank(message = "query is required")
    private String query;

    /**
     * 是否返回各阶段调试指标
     */
    private Boolean debug;
}
