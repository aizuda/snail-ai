package com.aizuda.snail.ai.admin.dto;

import lombok.Data;

/**
 * JWT audience 载荷：封装 token 中携带的用户名。
 *
 * @author opensnail
 * @since 2025-07-12
 */
@Data
public class AudienceDTO {
    private String username;
}
