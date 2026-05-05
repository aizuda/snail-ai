package com.aizuda.snail.ai.model.model;

import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;

/**
 * author: opensnail
 * date: 2026-03-04
 */
public interface Model {

    boolean supports(String modelKey);

    void setModelConfigInfo(ModelConfigInfoDTO modelConfigInfo);
}
