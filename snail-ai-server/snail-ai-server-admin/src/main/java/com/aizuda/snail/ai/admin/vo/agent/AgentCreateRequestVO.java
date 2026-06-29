package com.aizuda.snail.ai.admin.vo.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能体快速创建请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AgentCreateRequestVO {

    /** 智能体描述（用于 AI 自动生成配置） */
    @NotBlank(message = "description is required")
    private String description;
}
