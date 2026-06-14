package com.aizuda.snail.ai.common.openapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OpenAPI 嵌入式对话 token 请求。
 */
@Data
public class OpenApiEmbedTokenRequest {

    @NotBlank(message = "openId is required")
    private String openId;

    private String trustedCredential;

    @Min(value = 60, message = "ttlSeconds must be greater than or equal to 60")
    @Max(value = 86400, message = "ttlSeconds must be less than or equal to 86400")
    private Integer ttlSeconds;
}
