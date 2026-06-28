package com.aizuda.snail.ai.persistence.openapi.mapper;

import com.aizuda.snail.ai.persistence.openapi.po.OpenApiUserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpenApiUserMapper extends BaseMapper<OpenApiUserPO> {
}
