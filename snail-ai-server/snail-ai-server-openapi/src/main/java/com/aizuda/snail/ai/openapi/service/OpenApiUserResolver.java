package com.aizuda.snail.ai.openapi.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.persistence.admin.mapper.UserMapper;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.openapi.mapper.OpenApiUserMapper;
import com.aizuda.snail.ai.persistence.openapi.po.OpenApiUserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OpenAPI 用户身份解析（appId + openId -> 平台用户）
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class OpenApiUserResolver {

    private final OpenApiUserMapper openApiUserMapper;
    private final UserMapper userMapper;

    /**
     * 解析平台用户
     *
     * @param appId  应用 ID
     * @param openId 开放平台用户标识
     * @return 关联的平台用户
     */
    public UserPO resolvePlatformUser(String appId, String openId) {
        return resolveUser(appId, openId).getPlatformUser();
    }

    /**
     * 解析开放用户与平台用户的组合
     *
     * @param appId  应用 ID
     * @param openId 开放平台用户标识
     * @return 开放用户与平台用户的组合；openId 缺失或无效时抛出认证异常
     */
    public OpenApiResolvedUser resolveUser(String appId, String openId) {
        if (StrUtil.isBlank(openId)) {
            throw new SnailAiAuthenticationException("缺少 openId 参数");
        }

        OpenApiUserPO openApiUser = openApiUserMapper.selectOne(
                new LambdaQueryWrapper<OpenApiUserPO>()
                        .eq(OpenApiUserPO::getAppId, appId)
                        .eq(OpenApiUserPO::getOpenId, openId));
        if (openApiUser == null) {
            throw new SnailAiAuthenticationException("OpenId 无效: {}", openId);
        }

        UserPO userPO = userMapper.selectById(openApiUser.getPlatformUserId());
        if (userPO == null) {
            throw new SnailAiAuthenticationException("关联用户不存在");
        }
        return new OpenApiResolvedUser(openApiUser, userPO);
    }

    @Getter
    @AllArgsConstructor
    public static class OpenApiResolvedUser {
        private final OpenApiUserPO openApiUser;
        private final UserPO platformUser;
    }
}
