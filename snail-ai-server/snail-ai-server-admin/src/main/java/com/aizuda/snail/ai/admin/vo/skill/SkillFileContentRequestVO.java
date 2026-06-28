package com.aizuda.snail.ai.admin.vo.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill 文件内容，用于 GET/PUT /skill/{id}/files/content
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillFileContentRequestVO {

    private String content;
    private String encoding; // "utf-8" | "base64"
    private Long size;
}
