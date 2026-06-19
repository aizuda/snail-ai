package com.aizuda.snail.ai.persistence.admin.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息持久化对象
 * 表: sai_user
 *
 * @author opensnail
 * @date 2026-04-14
 */
@Data
@TableName("sai_user")
public class UserPO {

    /**
     * 用户ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户角色
     * 0: 普通用户
     * 1: 高级用户
     * 2: 管理员
     * 3: 超级管理员
     */
    private Integer role;

    /**
     * 用户名
     * 登录时使用的唯一用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 电子邮箱
     * 用户的电子邮件地址
     * 用于密码重置、通知等
     */
    private String email;

    /**
     * 密码 (加密存储)
     * 用户登录密码的哈希值
     * 使用bcrypt或类似算法加密
     */
    private String password;

    /**
     * 头像资源ID
     * 关联 sai_resource.id
     */
    private Long resourceId;

    /**
     * 创建时间
     * 用户账户首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 用户信息最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
