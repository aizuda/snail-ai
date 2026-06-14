package com.aizuda.snail.ai.admin.vo.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkillResponseVO {

    private Long id;

    private String name;

    private String description;

    private String fileName;

    private Long fileSize;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
