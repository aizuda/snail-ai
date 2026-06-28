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

    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    @NotBlank(message = "query is required")
    private String query;

    private Boolean debug;
}
