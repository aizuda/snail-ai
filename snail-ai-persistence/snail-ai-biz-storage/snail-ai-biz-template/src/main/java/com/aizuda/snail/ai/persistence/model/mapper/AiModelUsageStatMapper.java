package com.aizuda.snail.ai.persistence.model.mapper;

import com.aizuda.snail.ai.persistence.model.po.AiModelUsageStatPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 模型使用统计 Mapper接口
 */
@Mapper
public interface AiModelUsageStatMapper extends BaseMapper<AiModelUsageStatPO> {

}
