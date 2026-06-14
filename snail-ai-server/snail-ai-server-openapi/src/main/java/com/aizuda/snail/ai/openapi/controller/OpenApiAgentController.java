package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserAgentRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.openapi.service.OpenApiAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenAPI Agent 查询接口
 *
 * @author opensnail
 * @date 2026-04-24
 */
@RestController
@RequiredArgsConstructor
public class OpenApiAgentController {

    private final OpenApiAgentService openApiAgentService;

    @GetMapping(OpenApiPathConstants.OPEN_API_AGENTS)
    public PageResult<List<OpenApiAgentVO>> listAgents(@Validated OpenApiAgentQueryRequest request) {
        return openApiAgentService.listAgents(request);
    }

    @GetMapping(OpenApiPathConstants.OPEN_API_AGENT)
    public Result<OpenApiAgentVO> getAgent(@Validated OpenApiAgentIdentityRequest request) {
        return Result.ok(openApiAgentService.getAgent(request.getAgentId()));
    }

    @GetMapping(OpenApiPathConstants.OPEN_API_USER_AGENTS)
    public Result<List<OpenApiAgentVO>> listUserAgents(@Validated OpenApiUserQueryRequest request) {
        return Result.ok(openApiAgentService.listUserAgents(request.getOpenId()));
    }

    @PostMapping(OpenApiPathConstants.OPEN_API_USER_AGENT)
    public Result<Void> subscribeAgent(@RequestBody @Validated OpenApiUserAgentRequest request) {
        openApiAgentService.subscribeAgent(request.getOpenId(), request.getAgentId());
        return Result.ok(null);
    }

    @DeleteMapping(OpenApiPathConstants.OPEN_API_USER_AGENT)
    public Result<Void> unsubscribeAgent(@Validated OpenApiUserAgentRequest request) {
        openApiAgentService.unsubscribeAgent(request.getOpenId(), request.getAgentId());
        return Result.ok(null);
    }
}
