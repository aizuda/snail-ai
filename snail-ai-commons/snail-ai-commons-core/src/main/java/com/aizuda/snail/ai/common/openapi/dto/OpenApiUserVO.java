package com.aizuda.snail.ai.common.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI 用户信息响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiUserVO {

    private String openId;

    private String externalId;

    private String nickname;

    private String avatarUrl;

    private boolean created;
}
