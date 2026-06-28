package com.aizuda.snail.ai.admin.vo.skill;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SkillCreateRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class SkillCreateRequestVO {

    @NotBlank(message = "name is required")
    private String name;

    private String description;
}
