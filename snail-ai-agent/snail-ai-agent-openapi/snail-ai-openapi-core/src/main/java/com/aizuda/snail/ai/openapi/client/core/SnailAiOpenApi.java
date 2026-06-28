package com.aizuda.snail.ai.openapi.client.core;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationClearRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatStreamEvent;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Snail AI OpenAPI Fluent Builder 门面
 * <p>
 * 使用示例：
 * <pre>
 * // 流式对话
 * SnailAiOpenApi.chat(agentId)
 *     .conversationId("conv-123")
 *     .content("你好")
 *     .listener(myListener)
 *     .stream();
 *
 * // 同步对话
 * ChatSyncResponse resp = SnailAiOpenApi.chat(agentId)
 *     .conversationId("conv-123")
 *     .content("你好")
 *     .execute();
 * </pre>
 *
 * @author opensnail
 * @date 2026-04-24
 */
public class SnailAiOpenApi {

    private static OpenApiChatClient chatClient;
    private static OpenApiConversationClient conversationClient;
    private static OpenApiAgentClient agentClient;
    private static OpenApiUserClient userClient;

    public static void init(OpenApiChatClient chat, OpenApiConversationClient conv,
                            OpenApiAgentClient agent, OpenApiUserClient user) {
        chatClient = chat;
        conversationClient = conv;
        agentClient = agent;
        userClient = user;
    }

    // ==================== Chat ====================

    public static ChatBuilder chat(Long agentId) {
        return new ChatBuilder(agentId);
    }

    public static class ChatBuilder {
        private final Long agentId;
        private String openId;
        private String conversationId;
        private String content;
        private List<Long> disabledMcpServerIds;
        private List<Long> disabledSkillIds;


        ChatBuilder(Long agentId) {
            this.agentId = agentId;
        }

        public ChatBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public ChatBuilder openId(String openId) {
            this.openId = openId;
            return this;
        }

        public ChatBuilder content(String content) {
            this.content = content;
            return this;
        }

        public ChatBuilder disabledMcpServerIds(List<Long> ids) {
            this.disabledMcpServerIds = ids;
            return this;
        }

        public ChatBuilder disabledSkillIds(List<Long> ids) {
            this.disabledSkillIds = ids;
            return this;
        }

        public Flux<OpenApiChatStreamEvent> stream() {
            checkClient();
            return chatClient.chatStream(buildRequest());
        }

        public OpenApiChatSyncResponse execute() {
            checkClient();
            Result<OpenApiChatSyncResponse> result = chatClient.chatSync(buildRequest());
            if (result.getStatus() != 1) {
                throw new SnailAiException("Chat failed: " + result.getMessage());
            }
            return result.getData();
        }

        private OpenApiChatRequest buildRequest() {
            if (openId == null || openId.isBlank()) {
                throw new IllegalStateException("openId is required for OpenAPI chat");
            }
            OpenApiChatRequest req = new OpenApiChatRequest();
            req.setAgentId(agentId);
            req.setOpenId(openId);
            req.setConversationId(conversationId);
            req.setContent(content);
            req.setDisabledMcpServerIds(disabledMcpServerIds);
            req.setDisabledSkillIds(disabledSkillIds);
            return req;
        }

        private void checkClient() {
            if (chatClient == null) {
                throw new IllegalStateException("SnailAiOpenApi not initialized. Use @EnableSnailAiOpenApi or call SnailAiOpenApi.init()");
            }
        }
    }

    // ==================== Conversations ====================

    public static ConversationBuilder conversations(Long agentId) {
        return new ConversationBuilder(agentId);
    }

    public static class ConversationBuilder {
        private final Long agentId;
        private String openId;
        private String conversationId;

        ConversationBuilder(Long agentId) {
            this.agentId = agentId;
        }

        public ConversationBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public ConversationBuilder openId(String openId) {
            this.openId = openId;
            return this;
        }

        public PageResult<List<OpenApiConversationVO>> list() {
            return list(1, 20);
        }

        public PageResult<List<OpenApiConversationVO>> list(int page, int size) {
            checkClient();
            requireOpenId();
            OpenApiConversationQueryRequest request = new OpenApiConversationQueryRequest();
            request.setAgentId(agentId);
            request.setOpenId(openId);
            request.setPage(page);
            request.setSize(size);
            return conversationClient.listConversations(request);
        }

        public Result<OpenApiConversationVO> create(String title) {
            checkClient();
            requireOpenId();
            OpenApiCreateConversationRequest request = new OpenApiCreateConversationRequest();
            request.setAgentId(agentId);
            request.setOpenId(openId);
            request.setTitle(title);
            return conversationClient.createConversation(request);
        }

        public Result<Void> delete() {
            checkClient();
            requireOpenId();
            OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
            request.setAgentId(agentId);
            request.setConversationId(conversationId);
            request.setOpenId(openId);
            return conversationClient.deleteConversation(request);
        }

        public Result<Void> clear() {
            checkClient();
            requireOpenId();
            OpenApiConversationClearRequest request = new OpenApiConversationClearRequest();
            request.setAgentId(agentId);
            request.setOpenId(openId);
            return conversationClient.clearConversations(request);
        }

        public Result<List<OpenApiMessageVO>> messages() {
            checkClient();
            requireOpenId();
            OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
            request.setAgentId(agentId);
            request.setConversationId(conversationId);
            request.setOpenId(openId);
            return conversationClient.getMessages(request);
        }

        private void checkClient() {
            if (conversationClient == null) {
                throw new IllegalStateException("SnailAiOpenApi not initialized. Use @EnableSnailAiOpenApi or call SnailAiOpenApi.init()");
            }
        }

        private void requireOpenId() {
            if (openId == null || openId.isBlank()) {
                throw new IllegalStateException("openId is required for OpenAPI conversation operations");
            }
        }
    }

    // ==================== Agents ====================

    public static Result<List<OpenApiAgentVO>> listAgents() {
        if (agentClient == null) {
            throw new IllegalStateException("SnailAiOpenApi not initialized");
        }
        OpenApiAgentQueryRequest request = new OpenApiAgentQueryRequest();
        return agentClient.listAgents(request);
    }

    public static Result<OpenApiAgentVO> getAgent(Long agentId) {
        if (agentClient == null) {
            throw new IllegalStateException("SnailAiOpenApi not initialized");
        }
        OpenApiAgentIdentityRequest request = new OpenApiAgentIdentityRequest();
        request.setAgentId(agentId);
        return agentClient.getAgent(request);
    }

    // ==================== User ====================

    public static UserRegisterBuilder registerUser() {
        return new UserRegisterBuilder();
    }

    public static Result<OpenApiUserVO> getUser(String openId) {
        if (userClient == null) {
            throw new IllegalStateException("SnailAiOpenApi not initialized");
        }
        OpenApiUserQueryRequest request = new OpenApiUserQueryRequest();
        request.setOpenId(openId);
        return userClient.getUser(request);
    }

    public static class UserRegisterBuilder {
        private String externalId;
        private String nickname;

        public UserRegisterBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public UserRegisterBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public OpenApiUserVO execute() {
            if (userClient == null) {
                throw new IllegalStateException("SnailAiOpenApi not initialized");
            }
            OpenApiUserRegisterRequest request = new OpenApiUserRegisterRequest();
            request.setExternalId(externalId);
            request.setNickname(nickname);
            Result<OpenApiUserVO> result = userClient.register(request);
            if (result.getStatus() != 1) {
                throw new SnailAiException("User register failed: " + result.getMessage());
            }
            return result.getData();
        }
    }
}
