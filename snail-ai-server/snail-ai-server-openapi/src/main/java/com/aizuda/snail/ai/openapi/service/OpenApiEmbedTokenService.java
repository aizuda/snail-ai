package com.aizuda.snail.ai.openapi.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.dto.AudienceDTO;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenResponse;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.openapi.service.OpenApiUserResolver.OpenApiResolvedUser;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * OpenAPI 嵌入式对话 token 服务。
 */
@Service
@RequiredArgsConstructor
public class OpenApiEmbedTokenService {

    private static final int DEFAULT_TOKEN_TTL_SECONDS = 3600;
    private static final String AUTH_HEADER = "Snail-Ai-Auth";

    private final OpenApiUserResolver openApiUserResolver;
    private final ResourceMapper resourceMapper;

    public OpenApiEmbedTokenResponse createEmbedToken(OpenApiEmbedTokenRequest request) {
        OpenApiSessionUtils.OpenApiSession session = OpenApiSessionUtils.current();
        OpenApiResolvedUser resolvedUser = openApiUserResolver.resolveUser(session.getAppId(), request.getOpenId());
        UserPO requestUser = resolvedUser.getPlatformUser();
        int ttlSeconds = request.getTtlSeconds() == null ? DEFAULT_TOKEN_TTL_SECONDS : request.getTtlSeconds();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
        String token = createUserAuthToken(requestUser, session, request, expiresAt);

        return OpenApiEmbedTokenResponse.builder()
                .token(token)
                .tokenType(AUTH_HEADER)
                .authHeader(AUTH_HEADER)
                .expiresAt(expiresAt)
                .ttlSeconds(ttlSeconds)
                .openId(request.getOpenId())
                .nickname(requestUser.getNickname())
                .avatarUrl(resolveAvatarUrl(requestUser))
                .build();
    }

    private String resolveAvatarUrl(UserPO user) {
        if (user == null || user.getResourceId() == null) {
            return null;
        }
        ResourcePO resource = resourceMapper.selectById(user.getResourceId());
        return resource != null ? resource.getAccessUrl() : null;
    }

    private String createUserAuthToken(UserPO user,
                                       OpenApiSessionUtils.OpenApiSession session,
                                       OpenApiEmbedTokenRequest request,
                                       LocalDateTime expiresAt) {
        if (user == null || StrUtil.isBlank(user.getUsername())) {
            throw new SnailAiException("用户认证信息不完整");
        }
        if (StrUtil.isBlank(session.getAppToken())) {
            throw new SnailAiException("OpenAPI 应用认证信息不完整");
        }

        AudienceDTO audience = new AudienceDTO();
        audience.setUsername(user.getUsername());

        Date expires = Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant());
        JWTCreator.Builder builder = JWT.create()
                .withExpiresAt(expires)
                .withAudience(JsonUtil.toJsonString(audience))
                .withClaim("appId", session.getAppId())
                .withClaim("openId", request.getOpenId());
        if (StrUtil.isNotBlank(request.getTrustedCredential())) {
            builder.withClaim("trustedCredential", request.getTrustedCredential());
        }
        return builder.sign(Algorithm.HMAC256(session.getAppToken()));
    }
}
