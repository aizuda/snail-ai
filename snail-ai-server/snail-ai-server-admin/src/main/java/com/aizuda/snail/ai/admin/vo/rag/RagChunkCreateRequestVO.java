package com.aizuda.snail.ai.admin.vo.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * RagChunkCreateRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class RagChunkCreateRequestVO {

    @JsonAlias("knowledgeId")
    private Long ragId;
    private Long documentId;
    private String content;
}
