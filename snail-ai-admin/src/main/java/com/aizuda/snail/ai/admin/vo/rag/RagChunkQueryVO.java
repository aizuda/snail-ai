package com.aizuda.snail.ai.admin.vo.rag;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RagChunkQueryVO extends BaseQueryVO {

    private Long documentId;

    @JsonAlias("knowledgeId")
    private Long ragId;
}
