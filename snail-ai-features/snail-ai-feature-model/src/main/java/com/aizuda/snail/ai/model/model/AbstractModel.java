package com.aizuda.snail.ai.model.model;

import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;

/**
 * author: opensnail
 * date: 2026-03-04
 */
public abstract class AbstractModel implements Model {
    protected ModelConfigInfoDTO modelConfigInfo;

    @Override
    public void setModelConfigInfo(ModelConfigInfoDTO modelConfigInfo) {
        this.modelConfigInfo = modelConfigInfo;
    }
}
