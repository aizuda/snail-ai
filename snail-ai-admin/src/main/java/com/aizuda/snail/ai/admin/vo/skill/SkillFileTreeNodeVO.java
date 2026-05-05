package com.aizuda.snail.ai.admin.vo.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Skill 文件树节点，用于 GET /skill/{id}/files 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillFileTreeNodeVO {

    private String name;
    private String type; // "file" | "directory"
    private Long size;
    private List<SkillFileTreeNodeVO> children;
}
