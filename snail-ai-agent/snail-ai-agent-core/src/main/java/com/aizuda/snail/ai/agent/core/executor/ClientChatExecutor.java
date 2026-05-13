package com.aizuda.snail.ai.agent.core.executor;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
import com.aizuda.snail.ai.agent.common.context.AgentChatContextThreadLocalAccessor;
import com.aizuda.snail.ai.agent.core.advisor.ClientAdvisorKeys;
import com.aizuda.snail.ai.agent.core.advisor.ClientStreamExecutionContext;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端 LLM 执行引擎：通过 {@link Advisor} 责任链管理 Prompt 注入、拦截器、思维链与 chunk 转发，
 * 自身仅负责构建 {@link ChatClient} 与流式调用。
 */
@Slf4j
public class ClientChatExecutor {

    private static final String DEFAULT_CONFIG_JSON = "{}";

    private final ObservationRegistry observationRegistry;
    private final String environment;
    private final Advisor[] defaultAdvisors;

    public ClientChatExecutor(ObservationRegistry observationRegistry,
                              String environment,
                              Advisor... defaultAdvisors) {
        this.observationRegistry = observationRegistry;
        this.environment = environment;
        this.defaultAdvisors = defaultAdvisors != null ? defaultAdvisors : new Advisor[0];
    }

    /**
     * 流式执行：Advisor 链负责 memory/history、拦截、thinking 上报与 chunk 回调。
     */
    public void executeStream(ChatDispatchRequest request, List<ToolCallback> tools,
                              Consumer<String> chunkConsumer,
                              Consumer<String> thinkingConsumer,
                              Consumer<ChatCompletionResult> completionConsumer,
                              Consumer<Throwable> errorConsumer) {
        try {
            ClientStreamExecutionContext state = new ClientStreamExecutionContext();
            executeFlux(request, tools, chunkConsumer, thinkingConsumer, state)
                    .contextWrite(ctx -> ctx.put(
                            AgentChatContextThreadLocalAccessor.KEY, buildContext(request)))
                    .doFinally(signal -> AgentChatContextHolder.clear())
                    .subscribe(
                            r -> {
                            },
                            errorConsumer,
                            () -> completionConsumer.accept(state.toCompletionResult())
                    );
        } catch (Exception e) {
            log.error("Failed to execute chat", e);
            AgentChatContextHolder.clear();
            errorConsumer.accept(e);
        }
    }

    /**
     * 返回原始响应流（文本 chunk 已由 {@link com.aizuda.snail.ai.agent.core.advisor.StreamChunkForwarderAdvisor} 转发）。
     */
    public Flux<ChatResponse> executeFlux(ChatDispatchRequest request, List<ToolCallback> tools,
                                          Consumer<String> chunkConsumer,
                                          Consumer<String> thinkingConsumer,
                                          ClientStreamExecutionContext state) {
        ChatClient chatClient = buildChatClient(request.getModelConfig(), tools);
        Prompt prompt = buildBasePrompt(request);

        return chatClient.prompt(prompt).advisors(a -> a
                .param(ClientAdvisorKeys.DISPATCH, request)
                .param(ClientAdvisorKeys.STREAM_STATE, state)
                .param(ClientAdvisorKeys.CHUNK_CONSUMER, chunkConsumer)
                .param(ClientAdvisorKeys.THINKING_CONSUMER, thinkingConsumer)).stream().chatResponse();
    }

    private AgentChatContextHolder.ChatContext buildContext(ChatDispatchRequest req) {
        AgentChatContextHolder.ChatContext ctx = new AgentChatContextHolder.ChatContext();
        if (req.getAgentConfig() != null) {
            ctx.setAgentId(req.getAgentConfig().getAgentId());
        }
        if (req.getUserInfo() != null) {
            ctx.setUserId(req.getUserInfo().getUserId());
        }
        ctx.setConversationId(req.getConversationId());
        if (req.getModelConfig() != null) {
            ctx.setModelKey(req.getModelConfig().getModelKey());
        }
        ctx.setTraceId(req.getTraceId());
        ctx.setRootSpanId(req.getRootSpanId());
        return ctx;
    }

    /**
     * 仅包含原始 system + user；memory 与 history 由 {@link com.aizuda.snail.ai.agent.core.advisor.MemoryInjectionAdvisor} 注入。
     */
    private Prompt buildBasePrompt(ChatDispatchRequest request) {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = request.getSystemPrompt() != null ? request.getSystemPrompt() : "";


        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (request.getUserMessage() != null && !request.getUserMessage().isEmpty()) {
            messages.add(new UserMessage(request.getUserMessage()));
        }
        return new Prompt(messages);
    }

    private ChatClient buildChatClient(ChatDispatchRequest.ModelConfig modelConfig, List<ToolCallback> tools) {
        if (modelConfig == null) {
            throw new IllegalArgumentException("Model config is required");
        }

        ConfigExtAttrsDTO extConfig = modelConfig.getConfigJson();

        Long timeoutMs = extConfig.getTimeoutMs();
        long readTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : 60_000L;
        long connectTimeoutMs = Math.min(readTimeoutMs, 10_000L);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .baseUrl(modelConfig.getApiEndpoint())
                .apiKey(modelConfig.getApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory));
        if (extConfig.getCompletionsPath() != null && !extConfig.getCompletionsPath().isEmpty()) {
            apiBuilder.completionsPath(extConfig.getCompletionsPath());
        }
        OpenAiApi api = apiBuilder.build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .internalToolExecutionEnabled(true)  // ✅ 启用自动工具执行，支持 ReAct 循环
                .model(modelConfig.getModelKey())
                .streamUsage(true);  // ✅ 让 OpenAI API 在流式最终 chunk 中返回 usage

        applyConfigOptions(optionsBuilder, modelConfig.getConfigJson());

        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                // .observationRegistry(observationRegistry)
                .build();
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                // .observationRegistry(observationRegistry)
                .toolCallingManager(toolCallingManager)
                .build();

        List<ToolCallback> tracedTools = tools.stream()
                .map(TracingToolCallbackWrapper::new)
                .collect(java.util.stream.Collectors.toList());

        return ChatClient.builder(chatModel)
                .defaultAdvisors(defaultAdvisors)
                .defaultToolCallbacks(tracedTools)
                .defaultToolContext(new HashMap<>())
                .build();
    }

    private ConfigExtAttrsDTO parseExtConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return new ConfigExtAttrsDTO();
        }
        try {
            ConfigExtAttrsDTO dto = JsonUtil.parseObject(configJson, ConfigExtAttrsDTO.class);
            return dto != null ? dto : new ConfigExtAttrsDTO();
        } catch (Exception e) {
            log.warn("Failed to parse configJson: {}", configJson, e);
            return new ConfigExtAttrsDTO();
        }
    }

    private void applyConfigOptions(OpenAiChatOptions.Builder builder, ConfigExtAttrsDTO config) {
        try {
            if (config == null) {
                return;
            }
            if (config.getTemperature() != null) {
                builder.temperature(config.getTemperature());
            }
            if (config.getMaxTokens() != null) {
                builder.maxTokens(config.getMaxTokens());
            }
            if (config.getTopP() != null) {
                builder.topP(config.getTopP());
            }
            if (config.getFrequencyPenalty() != null) {
                builder.frequencyPenalty(config.getFrequencyPenalty());
            }
            if (config.getPresencePenalty() != null) {
                builder.presencePenalty(config.getPresencePenalty());
            }
            if (config.getStopSequences() != null && !config.getStopSequences().isEmpty()) {
                builder.stop(config.getStopSequences());
            }
            if (config.getSeed() != null) {
                builder.seed(config.getSeed().intValue());
            }
        } catch (Exception e) {
            log.warn("Failed to parse model config: {}", config, e);
        }
    }

    public record ChatCompletionResult(String fullText, String fullThinking,
                                       int promptTokens, int completionTokens, long durationMs) {
    }
}
