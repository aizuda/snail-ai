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
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequiredArgsConstructor
public class OpenApiEmbedController {

    private final OpenApiEmbedTokenService openApiEmbedTokenService;

    /**
     * 创建嵌入式对话 token（GET，参数在查询串）
     *
     * @param request 嵌入式 token 请求（openId、有效期等）
     * @return 嵌入式对话 token 响应
     */
    @GetMapping(OpenApiPathConstants.OPEN_API_EMBED_TOKEN)
    public Result<OpenApiEmbedTokenResponse> embedToken(@Validated OpenApiEmbedTokenRequest request) {
        return Result.ok(openApiEmbedTokenService.createEmbedToken(request));
    }

    /**
     * 创建嵌入式对话 token（POST，参数在请求体）
     *
     * @param request 嵌入式 token 请求（openId、有效期等）
     * @return 嵌入式对话 token 响应
     */
    @PostMapping(OpenApiPathConstants.OPEN_API_EMBED_TOKEN)
    public Result<OpenApiEmbedTokenResponse> embedTokenByBody(
            @RequestBody @Validated OpenApiEmbedTokenRequest request) {
        return Result.ok(openApiEmbedTokenService.createEmbedToken(request));
    }
}
