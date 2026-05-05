package com.aizuda.snail.ai.persistence.security;

import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;

/**
 * 用户会话工具类（ThreadLocal，与认证拦截器配合）
 *
 * @author opensnail
 * @date 2023-11-22 23:14:53
 * @since 2.4.0
 */
public final class UserSessionUtils {

    private static final ThreadLocal<UserPO> USER_SESSION = new ThreadLocal<>();

    /**
     * 设置当前用户会话
     */
    public static void setUserSession(UserPO userPO) {
        USER_SESSION.set(userPO);
    }

    /**
     * 获取当前用户会话
     */
    public static UserPO currentUserSession() {
        UserPO userPO = USER_SESSION.get();
        if (userPO == null) {
            throw new SnailAiAuthenticationException("未登录或登录已过期");
        }
        return userPO;
    }

    /**
     * 清除当前用户会话
     */
    public static void clearUserSession() {
        USER_SESSION.remove();
    }

    private UserSessionUtils() {
    }
}
