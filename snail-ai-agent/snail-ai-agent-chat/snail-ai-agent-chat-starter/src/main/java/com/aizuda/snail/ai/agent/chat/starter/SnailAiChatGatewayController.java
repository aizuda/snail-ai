package com.aizuda.snail.ai.agent.chat.starter;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatAuthorize;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatConstants;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatCredentialValidator;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatProperties;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatSession;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatSessionContext;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiAgentVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationClearRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserAgentRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiEmbedClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(SnailAiChatConstants.GATEWAY_PATH)
public class SnailAiChatGatewayController {

    private static final String CONFIG_KEY_GATEWAY_PATH = "gatewayPath";
    private static final String CONFIG_KEY_PAGE_TITLE = "pageTitle";
    private static final String CONFIG_KEY_LOGO = "logo";
    private static final String CONFIG_KEY_EMBED = "embed";
    private static final String CONFIG_KEY_EMBED_ENABLED = "enabled";
    private static final String CONFIG_KEY_SHOW_HEADER = "showHeader";
    private static final String CONFIG_KEY_SHOW_SIDEBAR_USER = "showSidebarUser";
    private static final String CONFIG_KEY_SHOW_AGENT_MARKET = "showAgentMarket";
    private static final String CONFIG_KEY_COMPACT_INPUT = "compactInput";
    private static final String CONFIG_KEY_LOCK_AGENT = "lockAgent";
    private static final String DEFAULT_LOGO = "";

    private final OpenApiAgentClient openApiAgentClient;
    private final OpenApiChatClient openApiChatClient;
    private final OpenApiConversationClient openApiConversationClient;
    private final OpenApiEmbedClient openApiEmbedClient;
    private final OpenApiResourceProxy openApiResourceProxy;
    private final SnailAiChatProperties chatProperties;
    private final SnailAiAgentProperties agentProperties;
    private final List<SnailAiChatCredentialValidator> credentialValidators;

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(CONFIG_KEY_GATEWAY_PATH, SnailAiChatConstants.GATEWAY_PATH);
        config.put(CONFIG_KEY_PAGE_TITLE, StrUtil.blankToDefault(
                chatProperties.getUi().getPageTitle(),
                SnailAiChatProperties.DEFAULT_PAGE_TITLE));
        config.put(CONFIG_KEY_LOGO, StrUtil.nullToDefault(chatProperties.getUi().getLogo(), DEFAULT_LOGO));
        putEmbedConfig(config, chatProperties.getUi().getEmbed());
        return Result.ok(config);
    }

    @PostMapping("/session")
    public Result<OpenApiEmbedTokenResponse> session(@RequestBody @Valid OpenApiEmbedTokenRequest request) {
        request.setOpenId(StrUtil.trim(request.getOpenId()));
        request.setTrustedCredential(trimToNull(request.getTrustedCredential()));
        request.setTtlSeconds(chatProperties.getSession().getTokenTtlSeconds());
        validateSessionRequest(request);
        return openApiEmbedClient.embedToken(request);
    }

    @SnailAiChatAuthorize
    @GetMapping("/agents")
    public PageResult<List<OpenApiAgentVO>> listAgents(@Valid OpenApiAgentQueryRequest request) {
        PageResult<List<OpenApiAgentVO>> agentPage = openApiAgentClient.listAgents(request);
        List<OpenApiAgentVO> agents = requirePageData(agentPage);
        if (agents == null || agents.isEmpty()) {
            return agentPage;
        }
        Set<Long> subscribedIds = subscribedAgentIds();
        agents.forEach(agent -> agent.setSubscribed(subscribedIds.contains(agent.getId())));
        return agentPage;
    }

    @SnailAiChatAuthorize
    @GetMapping("/agent")
    public Result<OpenApiAgentVO> getAgent(@Valid OpenApiAgentIdentityRequest request) {
        return openApiAgentClient.getAgent(request);
    }

    @SnailAiChatAuthorize
    @GetMapping("/my-agents")
    public Result<List<OpenApiAgentVO>> myAgents() {
        return openApiAgentClient.listUserAgents(userQuery());
    }

    @SnailAiChatAuthorize
    @PostMapping("/agent/subscribe")
    public Result<Void> subscribeAgent(@RequestParam("agentId") Long agentId) {
        OpenApiUserAgentRequest request = new OpenApiUserAgentRequest();
        request.setOpenId(currentOpenId());
        request.setAgentId(agentId);
        return openApiAgentClient.subscribeAgent(request);
    }

    @SnailAiChatAuthorize
    @DeleteMapping("/agent/subscribe")
    public Result<Void> unsubscribeAgent(@RequestParam("agentId") Long agentId) {
        OpenApiUserAgentRequest request = new OpenApiUserAgentRequest();
        request.setOpenId(currentOpenId());
        request.setAgentId(agentId);
        return openApiAgentClient.unsubscribeAgent(request);
    }

    @SnailAiChatAuthorize
    @GetMapping("/conversations")
    public PageResult<List<OpenApiConversationVO>> listConversations(
            @RequestParam("agentId") Long agentId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "50") Integer size) {
        OpenApiConversationQueryRequest request = new OpenApiConversationQueryRequest();
        request.setAgentId(agentId);
        request.setOpenId(currentOpenId());
        request.setPage(page);
        request.setSize(size);
        return openApiConversationClient.listConversations(request);
    }

    @SnailAiChatAuthorize
    @PostMapping("/conversations")
    public Result<OpenApiConversationVO> createConversation(@RequestBody OpenApiCreateConversationRequest request) {
        request.setOpenId(currentOpenId());
        return openApiConversationClient.createConversation(request);
    }

    @SnailAiChatAuthorize
    @DeleteMapping(value = "/conversations", params = "conversationId")
    public Result<Void> deleteConversation(@RequestParam("agentId") Long agentId,
                                           @RequestParam("conversationId") String conversationId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(currentOpenId());
        return openApiConversationClient.deleteConversation(request);
    }

    @SnailAiChatAuthorize
    @DeleteMapping(value = "/conversations", params = "!conversationId")
    public Result<Void> clearConversations(@RequestParam("agentId") Long agentId) {
        OpenApiConversationClearRequest request = new OpenApiConversationClearRequest();
        request.setAgentId(agentId);
        request.setOpenId(currentOpenId());
        return openApiConversationClient.clearConversations(request);
    }

    @SnailAiChatAuthorize
    @GetMapping({"/messages", "/conversations/messages"})
    public Result<List<OpenApiMessageVO>> getMessages(@RequestParam("agentId") Long agentId,
                                                       @RequestParam("conversationId") String conversationId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(currentOpenId());
        return openApiConversationClient.getMessages(request);
    }

    @SnailAiChatAuthorize
    @GetMapping("/resource/{id}/preview")
    public ResponseEntity<byte[]> previewResource(@PathVariable("id") Long id) {
        return openApiResourceProxy.preview(id);
    }

    @SnailAiChatAuthorize
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> completions(@RequestBody OpenApiChatRequest request) {
        request.setOpenId(currentOpenId());
        return openApiChatClient.chatStream(request)
                .map(event -> ServerSentEvent.<String>builder(event.getData())
                        .event(event.getType())
                        .build());
    }

    private String currentOpenId() {
        String openId = SnailAiChatSessionContext.current().getOpenId();
        if (StrUtil.isBlank(openId)) {
            throw new SnailAiException("Token 缺少 openId");
        }
        return openId;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private void putEmbedConfig(Map<String, Object> config, SnailAiChatProperties.Embed embedProperties) {
        if (embedProperties == null) {
            return;
        }

        Map<String, Object> embed = new LinkedHashMap<>();
        putIfNotNull(embed, CONFIG_KEY_EMBED_ENABLED, embedProperties.getEnabled());
        putIfNotNull(embed, CONFIG_KEY_SHOW_HEADER, embedProperties.getShowHeader());
        putIfNotNull(embed, CONFIG_KEY_SHOW_SIDEBAR_USER, embedProperties.getShowSidebarUser());
        putIfNotNull(embed, CONFIG_KEY_SHOW_AGENT_MARKET, embedProperties.getShowAgentMarket());
        putIfNotNull(embed, CONFIG_KEY_COMPACT_INPUT, embedProperties.getCompactInput());
        putIfNotNull(embed, CONFIG_KEY_LOCK_AGENT, embedProperties.getLockAgent());
        if (!embed.isEmpty()) {
            config.put(CONFIG_KEY_EMBED, embed);
        }
    }

    private void putIfNotNull(Map<String, Object> config, String key, Boolean value) {
        if (value != null) {
            config.put(key, value);
        }
    }

    private void validateSessionRequest(OpenApiEmbedTokenRequest request) {
        if (credentialValidators == null || credentialValidators.isEmpty()) {
            return;
        }
        SnailAiChatSession session = SnailAiChatSession.builder()
                .appId(agentProperties.getAppId())
                .openId(request.getOpenId())
                .trustedCredential(request.getTrustedCredential())
                .build();
        for (SnailAiChatCredentialValidator validator : credentialValidators) {
            validator.validate(session);
        }
    }

    private <T> T requireData(Result<T> result) {
        if (result == null) {
            throw new SnailAiException("OpenAPI response is empty");
        }
        if (result.getStatus() != 1) {
            throw new SnailAiException(result.getMessage());
        }
        return result.getData();
    }

    private <T> T requirePageData(PageResult<T> result) {
        if (result == null) {
            throw new SnailAiException("OpenAPI response is empty");
        }
        if (result.getStatus() != 1) {
            throw new SnailAiException(result.getMessage());
        }
        return result.getData();
    }

    private OpenApiUserQueryRequest userQuery() {
        OpenApiUserQueryRequest request = new OpenApiUserQueryRequest();
        request.setOpenId(currentOpenId());
        return request;
    }

    private Set<Long> subscribedAgentIds() {
        try {
            List<OpenApiAgentVO> userAgents = requireData(openApiAgentClient.listUserAgents(userQuery()));
            if (userAgents == null || userAgents.isEmpty()) {
                return Set.of();
            }
            return userAgents.stream()
                    .map(OpenApiAgentVO::getId)
                    .collect(Collectors.toSet());
        } catch (RuntimeException ignored) {
            return Set.of();
        }
    }
}
