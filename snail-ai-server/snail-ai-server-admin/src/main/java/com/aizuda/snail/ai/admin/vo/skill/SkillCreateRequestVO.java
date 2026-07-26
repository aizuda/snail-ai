package com.aizuda.snail.ai.admin.vo.skill;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 技能新增请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class SkillCreateRequestVO {

    /** 技能名称 */
    @NotBlank(message = "name is required")
    private String name;

    /** 技能描述 */
    private String description;
}
