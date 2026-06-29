package com.aizuda.snail.ai.admin.vo.model;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 按供应商与模型类型查询模型参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelByProviderTypeQueryVO extends BaseQueryVO {

    /** 供应商标识 */
    private String providerKey;

    /** 模型类型 */
    private String modelType;
}
