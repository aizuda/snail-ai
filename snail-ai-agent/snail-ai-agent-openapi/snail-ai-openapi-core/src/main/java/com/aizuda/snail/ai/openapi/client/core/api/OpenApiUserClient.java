package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;

/**
 * OpenAPI 用户客户端接口，声明外部应用用户注册与查询的远程调用映射。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface OpenApiUserClient {

    /**
     * 注册外部应用用户
     *
     * @param request 注册请求
     * @return 用户视图对象
     */
    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER_REGISTER, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiUserVO> register(OpenApiUserRegisterRequest request);

    /**
     * 按 openId 查询用户
     *
     * @param request 查询请求
     * @return 用户视图对象
     */
    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER, method = OpenApiMapping.HttpMethod.GET)
    Result<OpenApiUserVO> getUser(OpenApiUserQueryRequest request);
}
