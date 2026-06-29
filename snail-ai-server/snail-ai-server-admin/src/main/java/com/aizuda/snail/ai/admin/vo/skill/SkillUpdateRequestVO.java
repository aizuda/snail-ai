package com.aizuda.snail.ai.admin.vo.skill;

import lombok.Data;

/**
 * 技能更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class SkillUpdateRequestVO {

    /** 技能名称 */
    private String name;

    /** 技能描述 */
    private String description;
}
