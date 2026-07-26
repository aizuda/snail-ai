package com.aizuda.snail.ai.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class UserInfoVO {

    private Long id;

    private Integer role;

    /** 角色展示名 */
    private String roleName;

    /** 登录名 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    private String email;

    /** 头像URL（通过 resourceId 解析） */
    private String avatarUrl;

    private Long tokens;

    private Integer totals;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireDt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createDt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateDt;

}
