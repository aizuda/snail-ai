package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户请求VO
 *
 * @author opensnail
 * @date 2025-04-28
 */
@Data
public class UserUpdateRequestVO {
    /**
     * 角色 (1=普通用户, 2=管理员)
     */
    @NotNull(message = "角色不能为空")
    private Integer role;
    
    /**
     * 邮箱（可选）
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 密码（可选，为空则不更新密码）
     */
    private String password;

    /**
     * 头像资源ID（优先使用，关联 sai_resource.id）
     */
    private Long resourceId;

    /**
     * 是否清空头像
     */
    private Boolean avatarCleared;
}
