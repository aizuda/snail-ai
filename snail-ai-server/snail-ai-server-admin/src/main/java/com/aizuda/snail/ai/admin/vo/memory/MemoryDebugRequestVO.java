package com.aizuda.snail.ai.admin.vo.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 记忆检索调试请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class MemoryDebugRequestVO {

    /** 用于检索的智能体（需在智能体上绑定本记忆配置） */
    @NotNull
    private Long agentId;

    /** 对话用户 ID（记忆实体） */
    @NotNull
    private Long userId;

    /** 检索查询文本 */
    @NotBlank
    private String query;

    /** 会话 ID */
    private String conversationId;
}
