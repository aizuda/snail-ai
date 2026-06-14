package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenResponse;
import com.aizuda.snail.ai.openapi.service.OpenApiEmbedTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI 嵌入式对话接口。
 */
@RestController
@RequiredArgsConstructor
public class OpenApiEmbedController {

    private final OpenApiEmbedTokenService openApiEmbedTokenService;

    @GetMapping(OpenApiPathConstants.OPEN_API_EMBED_TOKEN)
    public Result<OpenApiEmbedTokenResponse> embedToken(@Validated OpenApiEmbedTokenRequest request) {
        return Result.ok(openApiEmbedTokenService.createEmbedToken(request));
    }

    @PostMapping(OpenApiPathConstants.OPEN_API_EMBED_TOKEN)
    public Result<OpenApiEmbedTokenResponse> embedTokenByBody(
            @RequestBody @Validated OpenApiEmbedTokenRequest request) {
        return Result.ok(openApiEmbedTokenService.createEmbedToken(request));
    }
}
