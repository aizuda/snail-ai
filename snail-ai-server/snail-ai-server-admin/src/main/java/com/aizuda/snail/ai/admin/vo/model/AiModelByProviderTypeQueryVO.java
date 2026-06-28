package com.aizuda.snail.ai.admin.vo.model;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AiModelByProviderTypeQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelByProviderTypeQueryVO extends BaseQueryVO {

    private String providerKey;

    private String modelType;
}
