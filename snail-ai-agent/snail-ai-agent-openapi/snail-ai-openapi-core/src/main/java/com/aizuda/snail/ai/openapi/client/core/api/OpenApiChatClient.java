package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatStreamEvent;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceUploadRequest;
import reactor.core.publisher.Flux;

/**
 * OpenAPI Chat 客户端接口
 *
 * @author opensnail
 * @since 2026-04-24
 */
public interface OpenApiChatClient {

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CHAT, method = OpenApiMapping.HttpMethod.POST)
    Flux<OpenApiChatStreamEvent> chatStream(OpenApiChatRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_AGENT_CHAT_SYNC, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiChatSyncResponse> chatSync(OpenApiChatRequest request);

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_RESOURCE_UPLOAD, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiResourceResponse> uploadResource(OpenApiResourceUploadRequest request);
}
