package com.aizuda.snail.ai.admin.vo.model;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelByProviderTypeQueryVO extends BaseQueryVO {

    private String providerKey;

    private String modelType;
}
