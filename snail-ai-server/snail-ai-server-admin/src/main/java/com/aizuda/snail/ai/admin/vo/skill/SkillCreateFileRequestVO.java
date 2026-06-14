package com.aizuda.snail.ai.admin.vo.skill;

import lombok.Data;

/**
 * POST /skill/{id}/files 请求体
 */
@Data
public class SkillCreateFileRequestVO {

    private String path;
    private String type; // "file" | "directory"
}
