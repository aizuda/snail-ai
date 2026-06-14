package com.aizuda.snail.ai.admin.vo.skill;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SkillQueryVO extends BaseQueryVO {

    private String keyword;
}
