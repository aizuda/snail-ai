package com.aizuda.snail.ai.admin.vo.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RAG 知识库问答请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagQARequestVO {

    /** 知识库 ID */
    @JsonAlias("knowledgeId")
    @NotNull(message = "ragId is required")
    private Long ragId;

    /** 提问内容 */
    @NotBlank(message = "query is required")
    private String query;

    /** 会话 ID（用于多轮对话上下文） */
    private String conversationId;
}
