package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员授权用户请求：指定账号及授予的配额 / 有效天数。
 *
 * @author opensnail
 * @since 2025-07-13
 */
@Data
public class AuthorizeRequestVO {
    @NotBlank
    private String email;
    private Integer days;
    private Integer totals;
}
