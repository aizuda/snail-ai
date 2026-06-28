package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;

public interface OpenApiUserClient {

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER_REGISTER, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiUserVO> register(OpenApiUserRegisterRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER, method = OpenApiMapping.HttpMethod.GET)
    Result<OpenApiUserVO> getUser(OpenApiUserQueryRequest request);
}
