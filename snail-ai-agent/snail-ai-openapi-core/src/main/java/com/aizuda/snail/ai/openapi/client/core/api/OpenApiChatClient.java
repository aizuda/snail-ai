package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.openapi.client.core.listener.SseEventListener;
import com.aizuda.snail.ai.common.model.Result;

/**
 * OpenAPI Chat 客户端接口
 *
 * @author opensnail
 * @date 2026-04-24
 */
public interface OpenApiChatClient {

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CHAT, method = OpenApiMapping.HttpMethod.POST)
    void chatStream(OpenApiChatRequest request,
                    SseEventListener listener);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CHAT_SYNC, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiChatSyncResponse> chatSync(OpenApiChatRequest request);
}
