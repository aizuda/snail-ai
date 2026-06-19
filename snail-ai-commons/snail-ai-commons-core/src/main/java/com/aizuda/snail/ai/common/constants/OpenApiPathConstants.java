package com.aizuda.snail.ai.common.constants;

/**
 * OpenAPI 路径常量
 */
public interface OpenApiPathConstants {

    String OPEN_API_V1_PREFIX = "/openapi/v1";

    String OPEN_API_AGENTS = OPEN_API_V1_PREFIX + "/agents";
    String OPEN_API_AGENT = OPEN_API_V1_PREFIX + "/agent";
    String OPEN_API_USER_AGENTS = OPEN_API_V1_PREFIX + "/user/agents";
    String OPEN_API_USER_AGENT = OPEN_API_V1_PREFIX + "/user/agent";

    String OPEN_API_AGENT_CHAT = OPEN_API_V1_PREFIX + "/agent/chat";
    String OPEN_API_AGENT_CHAT_SYNC = OPEN_API_V1_PREFIX + "/agent/chat/sync";

    String OPEN_API_EMBED_TOKEN = OPEN_API_V1_PREFIX + "/embed-token";

    String OPEN_API_RESOURCE_PREVIEW = OPEN_API_V1_PREFIX + "/resources/{id}/preview";

    String OPEN_API_AGENT_CONVERSATIONS = OPEN_API_V1_PREFIX + "/agent/conversations";
    String OPEN_API_AGENT_CONVERSATION_MESSAGES = OPEN_API_V1_PREFIX + "/agent/conversations/messages";

    String OPEN_API_USER_REGISTER = OPEN_API_V1_PREFIX + "/user/register";
    String OPEN_API_USER = OPEN_API_V1_PREFIX + "/user";
}
