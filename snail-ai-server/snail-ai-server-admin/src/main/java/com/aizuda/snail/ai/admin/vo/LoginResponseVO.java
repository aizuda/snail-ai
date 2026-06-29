package com.aizuda.snail.ai.admin.vo;

import lombok.Data;

/**
 * 登录响应：返回用户名、昵称与访问 token。
 *
 * @author opensnail
 * @since 2025-07-12
 */
@Data
public class LoginResponseVO {
    public String username;
    public String nickname;
    public String token;
}
