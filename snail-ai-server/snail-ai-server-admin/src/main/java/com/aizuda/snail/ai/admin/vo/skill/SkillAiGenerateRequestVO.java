package com.aizuda.snail.ai.admin.vo.skill;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SkillAiGenerateRequestVO {

    private String name;

    private String description;

    @NotBlank(message = "requirement is required")
    private String requirement;

    private String currentContent;

    private String optimizeInstruction;
}
