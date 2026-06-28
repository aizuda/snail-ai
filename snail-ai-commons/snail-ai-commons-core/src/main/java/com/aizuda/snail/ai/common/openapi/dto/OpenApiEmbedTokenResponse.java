package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OpenAPI 嵌入式对话 token 响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiEmbedTokenResponse {

    private String token;

    private String tokenType;

    private String authHeader;

    private LocalDateTime expiresAt;

    private Integer ttlSeconds;

    private String openId;

    private String nickname;

    private String avatarUrl;
}
