package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2025-07-19
 */
@Data
public class UpdateConversationTitleRequestVO {
    @NotBlank
    private String title;
    @NotBlank
    private String conversationId;
}
