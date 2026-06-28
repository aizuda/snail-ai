package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentIdentityRequest;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserAgentRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;

import java.util.List;

/**
 * OpenAPI Agent 查询客户端接口
 *
 * @author opensnail
 * @date 2026-04-24
 */
public interface OpenApiAgentClient {

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENTS, method = OpenApiMapping.HttpMethod.GET)
    PageResult<List<OpenApiAgentVO>> listAgents(OpenApiAgentQueryRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT, method = OpenApiMapping.HttpMethod.GET)
    Result<OpenApiAgentVO> getAgent(OpenApiAgentIdentityRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER_AGENTS, method = OpenApiMapping.HttpMethod.GET)
    Result<List<OpenApiAgentVO>> listUserAgents(OpenApiUserQueryRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER_AGENT, method = OpenApiMapping.HttpMethod.POST)
    Result<Void> subscribeAgent(OpenApiUserAgentRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_USER_AGENT, method = OpenApiMapping.HttpMethod.DELETE)
    Result<Void> unsubscribeAgent(OpenApiUserAgentRequest request);
}
