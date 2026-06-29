package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.openapi.service.OpenApiUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 开放 API 用户接口控制器，提供外部应用接入场景下的用户注册与查询入口。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequiredArgsConstructor
public class OpenApiUserController {

    private final OpenApiUserService openApiUserService;

    /**
     * 注册（或幂等返回已存在的）外部应用用户
     *
     * @param request 注册请求，含外部用户标识、昵称、头像等
     * @return 用户视图对象，标识本次是否为新建
     */
    @PostMapping(OpenApiPathConstants.OPEN_API_USER_REGISTER)
    public Result<OpenApiUserVO> register(@RequestBody OpenApiUserRegisterRequest request) {
        return Result.ok(openApiUserService.register(request));
    }

    /**
     * 按 openId 查询当前应用下的用户信息
     *
     * @param request 查询请求，携带 openId
     * @return 用户视图对象
     */
    @GetMapping(OpenApiPathConstants.OPEN_API_USER)
    public Result<OpenApiUserVO> getUser(@Validated OpenApiUserQueryRequest request) {
        return Result.ok(openApiUserService.getByOpenId(request.getOpenId()));
    }
}
