package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;

import java.util.List;

/**
 * OpenAPI 会话管理客户端接口
 *
 * @author opensnail
 * @date 2026-04-24
 */
public interface OpenApiConversationClient {

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS, method = OpenApiMapping.HttpMethod.GET)
    PageResult<List<OpenApiConversationVO>> listConversations(OpenApiConversationQueryRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiConversationVO> createConversation(OpenApiCreateConversationRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS, method = OpenApiMapping.HttpMethod.DELETE)
    Result<Void> deleteConversation(OpenApiConversationIdentityRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CONVERSATION_MESSAGES, method = OpenApiMapping.HttpMethod.GET)
    Result<List<OpenApiMessageVO>> getMessages(OpenApiConversationIdentityRequest request);
}
