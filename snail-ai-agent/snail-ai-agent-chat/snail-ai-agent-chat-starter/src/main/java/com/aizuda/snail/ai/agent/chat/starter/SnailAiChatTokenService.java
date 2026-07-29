package com.aizuda.snail.ai.agent.chat.starter;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatSession;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * 对话网关 Token 服务，基于 JWT（HMAC256）校验嵌入会话令牌并解析出会话信息（openId、appId 等）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class SnailAiChatTokenService {

    public static final String AUTH_HEADER = "Snail-Ai-Auth";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_OPEN_ID = "openId";
    private static final String CLAIM_APP_ID = "appId";
    private static final String CLAIM_TRUSTED_CREDENTIAL = "trustedCredential";

    private final SnailAiAgentProperties agentProperties;

    public SnailAiChatTokenService(SnailAiAgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    /**
     * 校验会话令牌并解析为会话信息，校验失败/过期时抛出认证异常
     *
     * @param rawToken 原始令牌（可含 Bearer 前缀）
     * @return 解析出的对话会话
     */
    public SnailAiChatSession verify(String rawToken) {
        String token = normalizeToken(rawToken);
        if (StrUtil.isBlank(token)) {
            throw new SnailAiAuthenticationException("未登录或登录已过期");
        }
        if (StrUtil.isBlank(agentProperties.getToken())) {
            throw new SnailAiAuthenticationException("OpenAPI 应用认证信息不完整");
        }

        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(agentProperties.getToken())).build();
            DecodedJWT jwt = verifier.verify(token);
            String openId = claimAsString(jwt, CLAIM_OPEN_ID);
            if (StrUtil.isBlank(openId)) {
                throw new SnailAiAuthenticationException("Token 缺少 openId");
            }

            String appId = claimAsString(jwt, CLAIM_APP_ID);
            if (StrUtil.isBlank(appId)) {
                appId = agentProperties.getAppId();
            }
            if (StrUtil.isNotBlank(agentProperties.getAppId())
                    && StrUtil.isNotBlank(appId)
                    && !Objects.equals(agentProperties.getAppId(), appId)) {
                throw new SnailAiAuthenticationException("Token 应用不匹配");
            }

            return SnailAiChatSession.builder()
                    .token(token)
                    .appId(appId)
                    .openId(openId)
                    .trustedCredential(claimAsString(jwt, CLAIM_TRUSTED_CREDENTIAL))
                    .expiresAt(toInstant(jwt.getExpiresAt()))
                    .build();
        } catch (TokenExpiredException e) {
            throw new SnailAiAuthenticationException("登录已过期，请重新登录");
        } catch (JWTVerificationException e) {
            throw new SnailAiAuthenticationException("Token 验证失败");
        }
    }

    private String normalizeToken(String rawToken) {
        String token = StrUtil.trim(rawToken);
        if (StrUtil.startWithIgnoreCase(token, BEARER_PREFIX)) {
            return StrUtil.trim(token.substring(BEARER_PREFIX.length()));
        }
        return token;
    }

    private String claimAsString(DecodedJWT jwt, String name) {
        Claim claim = jwt.getClaim(name);
        return claim == null || claim.isNull() ? null : claim.asString();
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
