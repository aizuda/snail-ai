package com.aizuda.snail.ai.model.chat.openai.responses;

import com.aizuda.snail.ai.common.enums.model.HostedToolTypeEnum;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.HostedToolConfigDTO;
import com.aizuda.snail.ai.model.chat.ChatModelAdapter;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterException;
import com.aizuda.snail.ai.model.common.ModelErrorCode;
import com.aizuda.snail.ai.model.common.http.NewApiProviderRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.responses.HostedTool;
import org.springframework.ai.openai.responses.OpenAiResponsesChatModel;
import org.springframework.ai.openai.responses.OpenAiResponsesChatOptions;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI Responses 对话模型适配器。
 *
 * @author opensnail
 * @since 2026-11-14
 */
@Slf4j
public class OpenAiResponsesChatModelAdapter implements ChatModelAdapter, Ordered {

    public static final String ADAPTER_KEY = "openai-responses";

    private static final long DEFAULT_TIMEOUT_MS = 300_000L;
    private static final String RAW_TOOL_TYPE_KEY = "type";
    private static final String EXTRA_BODY_TOOLS_KEY = "tools";
    private static final String RESPONSE_FORMAT_TEXT = "text";
    private static final String RESPONSE_FORMAT_JSON_OBJECT = "json_object";

    private static final Set<String> REASONING_EFFORT_VALUES = Set.of(
            "none", "minimal", "low", "medium", "high", "xhigh");
    private static final Set<String> REASONING_SUMMARY_VALUES = Set.of("auto", "concise", "detailed");
    private static final Set<String> VERBOSITY_VALUES = Set.of("low", "medium", "high");
    private static final Set<String> TOOL_CHOICE_VALUES = Set.of("auto", "none", "required");
    private static final Set<String> IMAGE_SIZE_VALUES = Set.of(
            "auto", "1024x1024", "1536x1024", "1024x1536");
    /**
     * 本期不接入的厂商托管工具。Raw 和 extraBody 也不能绕过。
     */
    private static final Set<String> UNSUPPORTED_PROVIDER_TOOL_TYPES = Set.of(
            "mcp", "web_search", "web_search_preview", "file_search");

    @Override
    public String adapterKey() {
        return ADAPTER_KEY;
    }

    @Override
    public ChatModel create(ChatModelSpec spec) {
        log.debug("Creating OpenAI Responses ChatModel, provider={}, model={}",
                spec.providerKey(), spec.modelKey());
        OpenAiResponsesChatModel.Builder builder = OpenAiResponsesChatModel.builder()
                .options(buildOptions(spec));
        builder.httpClientBuilderCustomizer(httpClientBuilder -> {
            httpClientBuilder.interceptor(ResponsesOutputTextCompatibilityInterceptor.instance());
            if (NewApiProviderRequestInterceptor.supports(spec.providerKey())) {
                httpClientBuilder.interceptor(NewApiProviderRequestInterceptor.instance());
            }
        });
        return builder.build();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private OpenAiResponsesChatOptions buildOptions(ChatModelSpec spec) {
        ConfigExtAttrsDTO config = spec.configJson();
        OpenAiResponsesChatOptions.Builder builder = OpenAiResponsesChatOptions.builder()
                .baseUrl(spec.baseUrl())
                .apiKey(spec.apiKey())
                .model(spec.modelKey())
                .timeout(Duration.ofMillis(readTimeoutMs(config)));
        if (config == null) {
            return builder.build();
        }

        applyCommonOptions(builder, config);
        applyResponsesOptions(builder, config);
        applyHostedTools(builder, config.getHostedTools());
        applyExtraBody(builder, config.getExtraBody());
        return builder.build();
    }

    private void applyCommonOptions(OpenAiResponsesChatOptions.Builder builder, ConfigExtAttrsDTO config) {
        if (config.getMaxRetries() != null) {
            requireNonNegative(config.getMaxRetries(), "maxRetries");
            builder.maxRetries(config.getMaxRetries());
        }
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getTopP() != null) {
            builder.topP(config.getTopP());
        }
        if (config.getMaxTokens() != null) {
            requirePositive(config.getMaxTokens(), "maxTokens");
            builder.maxOutputTokens(config.getMaxTokens());
        }
        if (config.getResponseFormat() != null && !config.getResponseFormat().isBlank()) {
            builder.responseFormat(toResponseFormat(config.getResponseFormat()));
        }
    }

    private void applyResponsesOptions(OpenAiResponsesChatOptions.Builder builder, ConfigExtAttrsDTO config) {
        String reasoningEffort = optionalEnumValue(
                config.getReasoningEffort(), REASONING_EFFORT_VALUES, "reasoningEffort");
        String reasoningSummary = optionalEnumValue(
                config.getReasoningSummary(), REASONING_SUMMARY_VALUES, "reasoningSummary");
        String verbosity = optionalEnumValue(config.getVerbosity(), VERBOSITY_VALUES, "verbosity");
        String toolChoice = optionalEnumValue(config.getToolChoice(), TOOL_CHOICE_VALUES, "toolChoice");

        if (reasoningEffort != null) {
            builder.reasoningEffort(reasoningEffort);
        }
        if (reasoningSummary != null) {
            builder.reasoningSummary(reasoningSummary);
        }
        if (verbosity != null) {
            builder.verbosity(verbosity);
        }
        if (toolChoice != null) {
            builder.toolChoice(toolChoice);
        }
        if (config.getParallelToolCalls() != null) {
            builder.parallelToolCalls(config.getParallelToolCalls());
        }
        if (config.getMaxToolCalls() != null) {
            requirePositive(config.getMaxToolCalls(), "maxToolCalls");
            builder.maxToolCalls(config.getMaxToolCalls());
        }
    }

    private void applyHostedTools(OpenAiResponsesChatOptions.Builder builder,
                                  List<HostedToolConfigDTO> hostedToolConfigs) {
        if (hostedToolConfigs == null || hostedToolConfigs.isEmpty()) {
            return;
        }
        List<HostedTool> hostedTools = new ArrayList<>(hostedToolConfigs.size());
        for (int index = 0; index < hostedToolConfigs.size(); index++) {
            HostedToolConfigDTO config = hostedToolConfigs.get(index);
            hostedTools.add(toHostedTool(config, index));
        }
        builder.hostedTools(hostedTools);
    }

    private HostedTool toHostedTool(HostedToolConfigDTO config, int index) {
        if (config == null) {
            throw invalidConfig("hostedTools[" + index + "] is required");
        }
        rejectUnsupportedProviderTool(config.getType(), "hostedTools[" + index + "].type");
        HostedToolTypeEnum type = HostedToolTypeEnum.fromValue(config.getType());
        if (type == null) {
            throw invalidConfig("Unsupported hostedTools[" + index + "].type=" + config.getType());
        }
        return switch (type) {
            case CODE_INTERPRETER -> new HostedTool.CodeInterpreter(trimToNull(config.getContainerId()));
            case IMAGE_GENERATION -> toImageGeneration(config, index);
            case RAW -> toRawTool(config, index);
        };
    }

    private HostedTool.ImageGeneration toImageGeneration(HostedToolConfigDTO config, int index) {
        String size = optionalEnumValue(
                config.getSize(), IMAGE_SIZE_VALUES, "hostedTools[" + index + "].size");
        return new HostedTool.ImageGeneration(trimToNull(config.getModel()), size);
    }

    private HostedTool.Raw toRawTool(HostedToolConfigDTO config, int index) {
        Map<String, Object> tool = config.getTool();
        if (tool == null || tool.isEmpty()) {
            throw invalidConfig("hostedTools[" + index + "].tool is required");
        }
        Object rawType = tool.get(RAW_TOOL_TYPE_KEY);
        if (!(rawType instanceof String type) || type.isBlank()) {
            throw invalidConfig("hostedTools[" + index + "].tool.type is required");
        }
        rejectUnsupportedProviderTool(type, "hostedTools[" + index + "].tool.type");
        return new HostedTool.Raw(new LinkedHashMap<>(tool));
    }

    private void applyExtraBody(OpenAiResponsesChatOptions.Builder builder, Map<String, Object> extraBody) {
        if (extraBody == null || extraBody.isEmpty()) {
            return;
        }
        rejectUnsupportedProviderTools(extraBody.get(EXTRA_BODY_TOOLS_KEY));
        builder.extraBody(extraBody);
    }

    private void rejectUnsupportedProviderTools(Object tools) {
        if (!(tools instanceof Collection<?> collection)) {
            return;
        }
        for (Object tool : collection) {
            if (!(tool instanceof Map<?, ?> toolMap)) {
                continue;
            }
            Object type = toolMap.get(RAW_TOOL_TYPE_KEY);
            if (type instanceof String typeValue) {
                rejectUnsupportedProviderTool(typeValue, "extraBody.tools[].type");
            }
        }
    }

    private void rejectUnsupportedProviderTool(String type, String field) {
        if (type == null) {
            return;
        }
        if (UNSUPPORTED_PROVIDER_TOOL_TYPES.contains(type.trim().toLowerCase(Locale.ROOT))) {
            throw invalidConfig(field + " is not supported");
        }
    }

    private OpenAiChatModel.ResponseFormat toResponseFormat(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        OpenAiChatModel.ResponseFormat.Type type = switch (normalized) {
            case RESPONSE_FORMAT_TEXT -> OpenAiChatModel.ResponseFormat.Type.TEXT;
            case RESPONSE_FORMAT_JSON_OBJECT -> OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT;
            default -> throw invalidConfig("Unsupported responseFormat=" + value);
        };
        return OpenAiChatModel.ResponseFormat.builder().type(type).build();
    }

    private String optionalEnumValue(String value, Set<String> allowedValues, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw invalidConfig("Unsupported " + field + "=" + value);
        }
        return normalized;
    }

    private long readTimeoutMs(ConfigExtAttrsDTO config) {
        if (config == null || config.getTimeoutMs() == null) {
            return DEFAULT_TIMEOUT_MS;
        }
        if (config.getTimeoutMs() <= 0) {
            throw invalidConfig("timeoutMs must be greater than 0");
        }
        return config.getTimeoutMs();
    }

    private void requirePositive(int value, String field) {
        if (value <= 0) {
            throw invalidConfig(field + " must be greater than 0");
        }
    }

    private void requireNonNegative(int value, String field) {
        if (value < 0) {
            throw invalidConfig(field + " must not be negative");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ModelAdapterException invalidConfig(String message) {
        return new ModelAdapterException(ModelErrorCode.INVALID_MODEL_CONFIG, message);
    }
}
