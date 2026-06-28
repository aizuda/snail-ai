package com.aizuda.snail.ai.admin.vo.memory;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemoryRequestVO {

    private Long agentId;
    private Long userId;
    private String conversationId;
    private List<MemoryChatMessageVO> messages;

    /** 默认 true：走 LLM 提取 */
    @Builder.Default private Boolean autoExtract = Boolean.TRUE;
}
