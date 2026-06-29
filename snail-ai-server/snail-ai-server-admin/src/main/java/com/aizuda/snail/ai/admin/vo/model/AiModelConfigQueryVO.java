package com.aizuda.snail.ai.admin.vo.model;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 模型配置分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelConfigQueryVO extends BaseQueryVO {

    /** 供应商标识 */
    private String providerKey;

    /** 模型类型 */
    private String modelType;

    /** 配置作用域 */
    private String scope;
}
