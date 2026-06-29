package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新会话标题请求：会话 ID 与新标题。
 *
 * @author opensnail
 * @since 2025-07-19
 */
@Data
public class UpdateConversationTitleRequestVO {
    @NotBlank
    private String title;
    @NotBlank
    private String conversationId;
}
