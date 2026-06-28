package com.aizuda.snail.ai.admin.vo.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RagSearchRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagSearchRequestVO {

    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    @NotBlank(message = "query is required")
    private String query;

    private Boolean debug;
}
