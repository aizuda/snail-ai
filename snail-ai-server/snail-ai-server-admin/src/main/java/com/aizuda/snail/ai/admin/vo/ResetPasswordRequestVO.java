package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求VO
 *
 * @author opensnail
 * @date 2025-04-27
 */
@Data
public class ResetPasswordRequestVO {
    /**
     * 新密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
