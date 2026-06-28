package com.aizuda.snail.ai.admin.vo.skill;

import lombok.Data;

/**
 * PUT /skill/{id}/files/rename 请求体
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class SkillRenameFileRequestVO {

    private String oldPath;
    private String newPath;
}
