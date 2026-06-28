package com.aizuda.snail.ai.agent.chat.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnailAiChatSession {

    private String token;

    private String appId;

    private String openId;

    private String trustedCredential;

    private Instant expiresAt;
}
