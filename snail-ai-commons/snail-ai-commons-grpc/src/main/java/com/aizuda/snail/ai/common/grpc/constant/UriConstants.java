package com.aizuda.snail.ai.common.grpc.constant;

/**
 * gRPC URI 路由路径常量
 */
public interface UriConstants {

    // Client → Server
    String BEAT = "/beat";
    String CALLBACK_CONVERSATION_CREATE = "/callback/conversation/create";
    String CALLBACK_CONVERSATION_RECORD = "/callback/conversation/record";
    String CALLBACK_MEMORY_RETRIEVE = "/callback/memory/retrieve";
    String CALLBACK_MEMORY_SHORT_TERM = "/callback/memory/short-term";
    String CALLBACK_SKILL_CONTENT = "/callback/skill/content";
    String CALLBACK_RAG_SEARCH = "/callback/rag/search";

    // Server → Client
    String CHAT_DISPATCH = "/chat/dispatch";
    String PING = "/ping";
}
