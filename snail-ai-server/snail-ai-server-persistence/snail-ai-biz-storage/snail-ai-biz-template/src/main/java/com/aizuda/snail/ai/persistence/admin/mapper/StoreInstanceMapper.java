package com.aizuda.snail.ai.persistence.admin.mapper;

import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 存储实例数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Mapper
public interface StoreInstanceMapper extends BaseMapper<StoreInstancePO> {
}
