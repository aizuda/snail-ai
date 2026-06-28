package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求VO
 *
 * @author opensnail
 * @date 2025-07-12
 */
@Data
public class LoginRequestVO {
    @NotBlank(message = "账号不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
