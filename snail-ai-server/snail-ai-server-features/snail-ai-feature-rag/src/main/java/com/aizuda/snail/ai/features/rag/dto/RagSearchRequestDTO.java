package com.aizuda.snail.ai.features.rag.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RagSearchRequestDTO {

    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    @NotBlank(message = "query is required")
    private String query;

    private Boolean debug;
}
