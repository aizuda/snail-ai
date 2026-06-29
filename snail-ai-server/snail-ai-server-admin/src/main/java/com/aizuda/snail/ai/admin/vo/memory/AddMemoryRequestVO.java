package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新增记忆请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemoryRequestVO {


    /** 智能体 ID */
    private Long agentId;
    /** 用户 ID */
    private Long userId;
    /** 会话 ID */
    private String conversationId;
    /** 对话消息列表 */
    private List<MemoryChatMessageVO> messages;

    /** 默认 true：走 LLM 提取 */
    @Builder.Default
    private Boolean autoExtract = Boolean.TRUE;
}
