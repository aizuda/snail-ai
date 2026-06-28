package com.aizuda.snail.ai.openapi.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.mapper.UserMapper;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.openapi.mapper.OpenApiUserMapper;
import com.aizuda.snail.ai.persistence.openapi.po.OpenApiUserPO;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.features.resource.enums.ResourceStorageTypeEnum;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiUserService {

    private static final String PASSWORD_SALT = "snail_ai_2026";

    private final OpenApiUserMapper openApiUserMapper;
    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;

    @Transactional(rollbackFor = Exception.class)
    public OpenApiUserVO register(OpenApiUserRegisterRequest request) {
        String appId = OpenApiSessionUtils.current().getAppId();

        if (StrUtil.isNotBlank(request.getExternalId())) {
            OpenApiUserPO existing = openApiUserMapper.selectOne(
                    new LambdaQueryWrapper<OpenApiUserPO>()
                            .eq(OpenApiUserPO::getAppId, appId)
                            .eq(OpenApiUserPO::getExternalId, request.getExternalId()));
            if (existing != null) {
                UserPO existingUser = userMapper.selectById(existing.getPlatformUserId());
                return buildVO(existing, false, resolveAvatarUrl(existing.getPlatformUserId()),
                        existingUser != null ? existingUser.getNickname() : null);
            }
        }

        String openId = UUID.randomUUID().toString().replace("-", "");

        UserPO platformUser = new UserPO();
        platformUser.setUsername(openId);
        platformUser.setNickname(request.getNickname());
        platformUser.setRole(RoleEnum.USER.getRoleId());
        platformUser.setPassword(encryptPassword(UUID.randomUUID().toString()));
        platformUser.setCreateDt(LocalDateTime.now());
        platformUser.setUpdateDt(LocalDateTime.now());
        userMapper.insert(platformUser);

        if (StrUtil.isNotBlank(request.getAvatarUrl())) {
            createExternalAvatarResource(platformUser, request.getAvatarUrl(), request.getNickname());
        }

        OpenApiUserPO openApiUser = OpenApiUserPO.builder()
                .appId(appId)
                .openId(openId)
                .platformUserId(platformUser.getId())
                .externalId(request.getExternalId())
                .createDt(LocalDateTime.now())
                .updateDt(LocalDateTime.now())
                .build();
        openApiUserMapper.insert(openApiUser);

        return buildVO(openApiUser, true, request.getAvatarUrl(), request.getNickname());
    }

    public OpenApiUserVO getByOpenId(String openId) {
        String appId = OpenApiSessionUtils.current().getAppId();
        OpenApiUserPO user = openApiUserMapper.selectOne(
                new LambdaQueryWrapper<OpenApiUserPO>()
                        .eq(OpenApiUserPO::getAppId, appId)
                        .eq(OpenApiUserPO::getOpenId, openId));
        if (user == null) {
            throw new SnailAiException("用户不存在: " + openId);
        }
        UserPO platformUser = userMapper.selectById(user.getPlatformUserId());
        return buildVO(user, false, resolveAvatarUrl(user.getPlatformUserId()),
                platformUser != null ? platformUser.getNickname() : null);
    }

    private void createExternalAvatarResource(UserPO platformUser, String avatarUrl, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        ResourcePO resource = ResourcePO.builder()
                .storageKey(avatarUrl)
                .originalName(StrUtil.blankToDefault(nickname, "user") + "_avatar")
                .fileSize(0L)
                .storageType(ResourceStorageTypeEnum.EXTERNAL.getValue())
                .accessUrl(avatarUrl)
                .bizType(ResourceBizTypeEnum.AVATAR.getValue())
                .bizId(platformUser.getId())
                .creatorId(platformUser.getId())
                .createDt(now)
                .updateDt(now)
                .build();
        resourceMapper.insert(resource);
        platformUser.setResourceId(resource.getId());
        userMapper.updateById(platformUser);
    }

    private String resolveAvatarUrl(Long platformUserId) {
        UserPO user = userMapper.selectById(platformUserId);
        if (user == null || user.getResourceId() == null) {
            return null;
        }
        ResourcePO resource = resourceMapper.selectById(user.getResourceId());
        return resource != null ? resource.getAccessUrl() : null;
    }

    private OpenApiUserVO buildVO(OpenApiUserPO po, boolean created, String avatarUrl, String nickname) {
        return OpenApiUserVO.builder()
                .openId(po.getOpenId())
                .externalId(po.getExternalId())
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .created(created)
                .build();
    }

    private String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(PASSWORD_SALT.getBytes());
            byte[] hash = digest.digest(password.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new SnailAiException("密码加密失败", e);
        }
    }
}
