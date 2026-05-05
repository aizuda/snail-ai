package com.aizuda.snail.ai.common.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取 Skill 内容请求
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillContentRequest {

    private Long skillId;
    private Long agentId;
}
