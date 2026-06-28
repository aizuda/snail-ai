package com.aizuda.snail.ai.admin.vo.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AgentCreateRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AgentCreateRequestVO {

    @NotBlank(message = "description is required")
    private String description;
}
