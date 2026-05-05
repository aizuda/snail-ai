package com.aizuda.snail.ai.persistence.model.mapper;

import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型提供商 Mapper接口
 */
@Mapper
public interface AiModelProviderMapper extends BaseMapper<AiModelProviderPO> {

}
