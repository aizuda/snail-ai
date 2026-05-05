package com.aizuda.snail.ai.persistence.model.mapper;

import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型配置 Mapper接口 (已优化)
 *
 * 设计理念:
 * - 所有查询逻辑使用 LambdaQueryWrapper 在 Service 层实现
 * - Mapper 层仅保留基础 CRUD 操作
 * - 优势: 类型安全、易维护、支持动态条件、避免 SQL 字符串拼接
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfigPO> {
}
