package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建用户请求VO
 *
 * @author opensnail
 * @date 2025-04-27
 */
@Data
public class UserCreateRequestVO {
    /**
     * 用户名（登录账号）
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 角色 (1=普通用户, 2=管理员)
     */
    @NotNull(message = "角色不能为空")
    private Integer role;

}
