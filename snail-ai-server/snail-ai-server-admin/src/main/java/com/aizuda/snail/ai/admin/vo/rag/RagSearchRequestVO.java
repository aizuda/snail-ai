package com.aizuda.snail.ai.admin.vo.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RAG 检索请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagSearchRequestVO {

    /** 知识库 ID */
    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    /** 检索查询语句 */
    @NotBlank(message = "query is required")
    private String query;

    /** 是否开启调试模式 */
    private Boolean debug;
}
