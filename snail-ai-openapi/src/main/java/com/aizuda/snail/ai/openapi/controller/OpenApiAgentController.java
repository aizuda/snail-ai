package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
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
    public Result<List<OpenApiAgentVO>> listAgents() {
        return Result.ok(openApiAgentService.listAgents());
    }

    @GetMapping(OpenApiPathConstants.OPEN_API_AGENT)
    public Result<OpenApiAgentVO> getAgent(@Validated OpenApiAgentIdentityRequest request) {
        return Result.ok(openApiAgentService.getAgent(request.getAgentId()));
    }
}
