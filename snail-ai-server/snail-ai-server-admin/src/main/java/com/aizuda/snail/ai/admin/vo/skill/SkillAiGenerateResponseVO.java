package com.aizuda.snail.ai.admin.vo.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAiGenerateResponseVO {

    private String name;

    private String description;

    private String skillContent;

    private List<String> warnings;
}
