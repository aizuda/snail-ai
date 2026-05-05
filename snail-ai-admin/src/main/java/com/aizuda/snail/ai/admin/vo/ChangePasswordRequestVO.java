package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自助修改密码请求VO
 *
 * @author opensnail
 * @date 2026-04-28
 */
@Data
public class ChangePasswordRequestVO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 18, message = "密码长度需在6-18位之间")
    private String newPassword;
}
