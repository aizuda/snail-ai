package com.aizuda.snail.ai.admin.vo.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMemoryRequestVO {

    private String query;
    @NotNull
    private Long agentId;
    @NotNull
    private Long userId;
    @NotBlank
    private String conversationId;

    @Builder.Default
    private Integer limit = 5;

    @Builder.Default
    private Boolean rerank = Boolean.FALSE;
}
