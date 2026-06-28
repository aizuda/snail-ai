package com.aizuda.snail.ai.persistence.skill.mapper;

import com.aizuda.snail.ai.persistence.skill.po.SkillFilePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 技能文件数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Mapper
public interface SkillFileMapper extends BaseMapper<SkillFilePO> {
}

