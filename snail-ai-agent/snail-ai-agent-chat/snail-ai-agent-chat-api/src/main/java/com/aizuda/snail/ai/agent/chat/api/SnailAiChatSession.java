package com.aizuda.snail.ai.agent.chat.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 对话会话信息，由网关 Token 校验后产生，承载当前用户的身份与凭证。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnailAiChatSession {

    /**
     * 原始会话令牌
     */
    private String token;

    /**
     * 所属应用 ID
     */
    private String appId;

    /**
     * 开放平台用户标识
     */
    private String openId;

    /**
     * 受信凭证（用于业务侧二次校验）
     */
    private String trustedCredential;

    /**
     * 令牌过期时间
     */
    private Instant expiresAt;
}
