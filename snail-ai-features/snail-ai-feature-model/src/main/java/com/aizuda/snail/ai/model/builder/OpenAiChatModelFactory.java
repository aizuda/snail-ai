package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.ModelCallException;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * OpenAI ChatModel 工厂实现
 * 支持 OpenAI 及兼容接口（火山云、通义千问等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatModelFactory implements ChatModelFactory {
    private final ObservationRegistry observationRegistry;

    @Override
    public String getSupportedProvider() {
        return "openai";
    }

    @Override
    public ChatModel createChatModel(String providerKey, String baseUrl, String apiKey,
                                     String modelKey, ConfigExtAttrsDTO configExtAttrsDTO) throws Exception {
        try {
            // 1. 验证配置
            if (!isConfigValid(baseUrl, apiKey)) {
                throw new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER,
                        "baseUrl 和 apiKey 不能为空");
            }


            log.debug("Creating OpenAiChatModel for provider: {}, model: {}, baseUrl: {}",
                    providerKey, modelKey, baseUrl);

            OpenAiChatOptions chatOptions = buildChatOptions(baseUrl, apiKey, modelKey, configExtAttrsDTO);

            // Spring AI 2.0.0-M7 使用 OpenAI Java SDK，客户端由 OpenAiChatOptions 统一初始化。
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .options(chatOptions)
                    .observationRegistry(observationRegistry)
                    .build();


            log.info("Successfully created OpenAiChatModel for model: {}", modelKey);
            return chatModel;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create OpenAiChatModel for model: {}", modelKey, e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_MODEL_BUILD_FAILED,
                    "无法创建ChatModel: " + e.getMessage(),
                    e
            );
        }
    }

    private OpenAiChatOptions buildChatOptions(String baseUrl, String apiKey, String modelKey, ConfigExtAttrsDTO config) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(modelKey);
        if (config == null) {
            return builder.build();
        }
        Long timeoutMs = config.getTimeoutMs();
        long readTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : 60_000L;
        builder.timeout(Duration.ofMillis(readTimeoutMs));
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
        if (config.getTopK() != null) {
            builder.extraBody(Map.of("top_k", config.getTopK()));
        }
        if (config.getCompletionsPath() != null && !config.getCompletionsPath().isBlank()
                && !"/v1/chat/completions".equals(config.getCompletionsPath())) {
            log.warn("Spring AI 2.0.0-M7 OpenAI SDK no longer supports per-model completionsPath directly: {}",
                    config.getCompletionsPath());
        }
        return builder.build();
    }
}
