package com.aizuda.snail.ai.common.openapi.dto;

import lombok.Data;

/**
 * OpenAPI 用户注册请求
 */
@Data
public class OpenApiUserRegisterRequest {

    private String externalId;

    private String nickname;

    /**
     * 外部头像URL
     */
    private String avatarUrl;
}
