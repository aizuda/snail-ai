package com.aizuda.snail.ai.persistence.openapi.mapper;

import com.aizuda.snail.ai.persistence.openapi.po.OpenApiUserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OpenAPI 接入用户数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Mapper
public interface OpenApiUserMapper extends BaseMapper<OpenApiUserPO> {
}
