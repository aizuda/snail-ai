package com.aizuda.snail.ai.admin.vo.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能体对话附件请求参数。
 *
 * @author opensnail
 * @since 2026-07-07
 */
@Data
public class AgentChatAttachmentVO {

    /** 资源 ID */
    @NotNull(message = "resourceId is required")
    private Long resourceId;

    /** 附件类型 */
    @NotBlank(message = "type is required")
    private String type;
}
