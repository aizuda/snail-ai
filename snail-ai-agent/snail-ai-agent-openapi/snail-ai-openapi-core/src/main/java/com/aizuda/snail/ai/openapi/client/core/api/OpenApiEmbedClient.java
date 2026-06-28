package com.aizuda.snail.ai.openapi.client.core.api;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenResponse;
import com.aizuda.snail.ai.openapi.client.core.annotation.OpenApiMapping;

/**
 * OpenAPI 嵌入式对话客户端接口。
 */
public interface OpenApiEmbedClient {

    @OpenApiMapping(path = OpenApiPathConstants.OPEN_API_EMBED_TOKEN, method = OpenApiMapping.HttpMethod.POST)
    Result<OpenApiEmbedTokenResponse> embedToken(OpenApiEmbedTokenRequest request);
}
