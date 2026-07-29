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
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationUpdateTitleRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiEmbedTokenResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceResponse;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiResourceUploadRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserAgentRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserQueryRequest;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiEmbedClient;
import com.aizuda.snail.ai.openapi.client.core.config.SnailAiOpenApiProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 嵌入式对话网关控制器，对前端暴露配置、会话、智能体、会话记录与流式对话等接口，统一转发至 OpenAPI 客户端。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(SnailAiChatConstants.GATEWAY_PATH)
public class SnailAiChatGatewayController {

    private static final String CONFIG_KEY_GATEWAY_PATH = "gatewayPath";
    private static final String CONFIG_KEY_PAGE_TITLE = "pageTitle";
    private static final String CONFIG_KEY_LOGO = "logo";
    private static final String CONFIG_KEY_RESOURCE_BASE_URL = "resourceBaseUrl";
    private static final String CONFIG_KEY_EMBED = "embed";
    private static final String CONFIG_KEY_EMBED_ENABLED = "enabled";
    private static final String CONFIG_KEY_SHOW_HEADER = "showHeader";
    private static final String CONFIG_KEY_SHOW_SIDEBAR_USER = "showSidebarUser";
    private static final String CONFIG_KEY_SHOW_AGENT_MARKET = "showAgentMarket";
    private static final String CONFIG_KEY_COMPACT_INPUT = "compactInput";
    private static final String CONFIG_KEY_LOCK_AGENT = "lockAgent";
    private static final String CONFIG_KEY_DEFAULT_AGENT_ID = "defaultAgentId";
    private static final String CONFIG_KEY_SHOW_MY_AGENTS = "showMyAgents";
    private static final String DEFAULT_LOGO = "";
    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final int HTTPS_DEFAULT_PORT = 443;

    private final OpenApiAgentClient openApiAgentClient;
    private final OpenApiChatClient openApiChatClient;
    private final OpenApiConversationClient openApiConversationClient;
    private final OpenApiEmbedClient openApiEmbedClient;
    private final SnailAiChatProperties chatProperties;
    private final SnailAiAgentProperties agentProperties;
    private final SnailAiOpenApiProperties openApiProperties;
    private final List<SnailAiChatCredentialValidator> credentialValidators;

    /** 返回聊天页前端初始化配置（网关路径、标题、Logo、资源地址与嵌入选项），无需登录态 */
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(CONFIG_KEY_GATEWAY_PATH, SnailAiChatConstants.GATEWAY_PATH);
        config.put(CONFIG_KEY_PAGE_TITLE, StrUtil.blankToDefault(
                chatProperties.getUi().getPageTitle(),
                SnailAiChatProperties.DEFAULT_PAGE_TITLE));
        config.put(CONFIG_KEY_LOGO, StrUtil.nullToDefault(chatProperties.getUi().getLogo(), DEFAULT_LOGO));
        config.put(CONFIG_KEY_RESOURCE_BASE_URL, resolveResourceBaseUrl());
        putEmbedConfig(config, chatProperties.getUi().getEmbed());
        return Result.ok(config);
    }

    /** 换取嵌入会话 Token：先经过可插拔的凭证校验器链，再向 OpenAPI 申请带 TTL 的会话令牌 */
    @PostMapping("/session")
    public Result<OpenApiEmbedTokenResponse> session(@RequestBody @Valid OpenApiEmbedTokenRequest request) {
        request.setOpenId(StrUtil.trim(request.getOpenId()));
        request.setTrustedCredential(trimToNull(request.getTrustedCredential()));
        request.setTtlSeconds(chatProperties.getSession().getTokenTtlSeconds());
        validateSessionRequest(request);
        log.info("嵌入会话 Token 签发, openId={}, ttlSeconds={}", request.getOpenId(), request.getTtlSeconds());
        return openApiEmbedClient.embedToken(request);
    }

    /** 分页查询智能体市场列表，并按当前用户订阅关系回填 subscribed 标记 */
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
    @PutMapping("/conversations")
    public Result<Void> updateConversationTitle(@RequestBody @Valid OpenApiConversationUpdateTitleRequest request) {
        request.setOpenId(currentOpenId());
        return openApiConversationClient.updateConversationTitle(request);
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

    /** 流式对话入口：将 OpenAPI 的流事件桥接为 SSE 下发前端，流异常会记录日志并透传给订阅方 */
    @SnailAiChatAuthorize
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> completions(@RequestBody OpenApiChatRequest request) {
        request.setOpenId(currentOpenId());
        log.info("流式对话开始, agentId={}, conversationId={}, openId={}",
                request.getAgentId(), request.getConversationId(), request.getOpenId());
        return openApiChatClient.chatStream(request)
                .map(event -> ServerSentEvent.<String>builder(event.getData())
                        .event(event.getType())
                        .build())
                .doOnError(e -> log.error("流式对话异常, agentId={}, conversationId={}",
                        request.getAgentId(), request.getConversationId(), e));
    }

    /** 上传对话资源（如图片附件），文件内容读入内存后转发 OpenAPI 落库 */
    @SnailAiChatAuthorize
    @PostMapping(value = "/resource/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OpenApiResourceResponse> uploadResource(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "bizType", required = false) String bizType,
                                                          @RequestParam(value = "bizId", required = false) Long bizId) {
        OpenApiResourceUploadRequest request = new OpenApiResourceUploadRequest();
        request.setOpenId(currentOpenId());
        request.setOriginalName(file.getOriginalFilename());
        request.setFileSize(file.getSize());
        request.setBizType(bizType);
        request.setBizId(bizId);
        try {
            request.setContent(file.getBytes());
        } catch (IOException e) {
            log.error("读取上传文件失败, fileName={}, size={}", file.getOriginalFilename(), file.getSize(), e);
            throw new SnailAiException("Failed to read uploaded file", e);
        }
        log.info("对话资源上传, fileName={}, size={}, bizType={}", file.getOriginalFilename(), file.getSize(), bizType);
        return openApiChatClient.uploadResource(request);
    }

    /** 从会话上下文取当前用户 openId；Token 中缺失说明会话不合法，直接拒绝 */
    private String currentOpenId() {
        String openId = SnailAiChatSessionContext.current().getOpenId();
        if (StrUtil.isBlank(openId)) {
            log.warn("会话 Token 缺少 openId，拒绝请求");
            throw new SnailAiException("Token 缺少 openId");
        }
        return openId;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    /** 组装 iframe 嵌入模式配置，仅下发显式配置过的开关项，未配置的交由前端默认值 */
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
        putIfNotNull(embed, CONFIG_KEY_DEFAULT_AGENT_ID, embedProperties.getDefaultAgentId());
        putIfNotNull(embed, CONFIG_KEY_SHOW_MY_AGENTS, embedProperties.getShowMyAgents());
        if (!embed.isEmpty()) {
            config.put(CONFIG_KEY_EMBED, embed);
        }
    }

    private void putIfNotNull(Map<String, Object> config, String key, Boolean value) {
        if (value != null) {
            config.put(key, value);
        }
    }

    private void putIfNotNull(Map<String, Object> config, String key, Long value) {
        if (value != null) {
            config.put(key, value);
        }
    }

    /** 解析资源访问基础地址：优先取显式配置，否则按 OpenAPI 服务端信息推导 */
    private String resolveResourceBaseUrl() {
        String configured = chatProperties.getUi().getResourceBaseUrl();
        if (StrUtil.isNotBlank(configured)) {
            return configured.replaceAll("/$", "");
        }
        return buildOpenApiBaseUrl();
    }

    /** 由 OpenAPI 配置推导基础 URL（协议 + 主机 + 非默认端口 + 前缀） */
    private String buildOpenApiBaseUrl() {
        String host = openApiProperties.getServerHost();
        if (StrUtil.isBlank(host)) {
            host = agentProperties.getServer().getHost();
        }

        String protocol = openApiProperties.isHttps() ? HTTPS_PROTOCOL : HTTP_PROTOCOL;
        StringBuilder url = new StringBuilder(protocol)
                .append("://")
                .append(host.replaceAll("/$", ""));
        appendPort(url);
        appendPrefix(url);
        return url.toString();
    }

    private void appendPort(StringBuilder url) {
        int port = openApiProperties.getWebPort();
        boolean defaultPort = (openApiProperties.isHttps() && port == HTTPS_DEFAULT_PORT)
                || (!openApiProperties.isHttps() && port == HTTP_DEFAULT_PORT);
        if (!defaultPort) {
            url.append(":").append(port);
        }
    }

    private void appendPrefix(StringBuilder url) {
        String prefix = openApiProperties.getPrefix();
        if (StrUtil.isBlank(prefix)) {
            return;
        }
        if (!prefix.startsWith("/")) {
            url.append("/");
        }
        url.append(prefix);
    }

    /** 依次执行宿主应用注册的凭证校验器；任一校验器抛异常即拒绝签发会话 Token */
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

    /** 查询当前用户已订阅的智能体 ID 集合；失败只影响 subscribed 标记展示，降级为空集不阻断列表 */
    private Set<Long> subscribedAgentIds() {
        try {
            List<OpenApiAgentVO> userAgents = requireData(openApiAgentClient.listUserAgents(userQuery()));
            if (userAgents == null || userAgents.isEmpty()) {
                return Set.of();
            }
            return userAgents.stream()
                    .map(OpenApiAgentVO::getId)
                    .collect(Collectors.toSet());
        } catch (RuntimeException e) {
            log.warn("查询用户订阅智能体失败，订阅标记降级为空: {}", e.getMessage());
            return Set.of();
        }
    }
}
