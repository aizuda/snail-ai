package com.aizuda.snail.ai.model.chat.openai;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.chat.ChatModelAdapter;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class OpenAiCompatibleChatModelAdapter implements ChatModelAdapter, Ordered {

    @Override
    public String adapterKey() {
        return ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER;
    }

    @Override
    public ChatModel create(ChatModelSpec spec) {
        return OpenAiChatModel.builder()
                .options(buildOptions(spec))
                .build();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private OpenAiChatOptions buildOptions(ChatModelSpec spec) {
        ConfigExtAttrsDTO config = spec.configJson();
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(spec.baseUrl())
                .apiKey(spec.apiKey())
                .model(spec.modelKey())
                .timeout(Duration.ofMillis(readTimeoutMs(config)));
        if (config == null) {
            return builder.build();
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
        if (config.getTopK() != null) {
            builder.topK(config.getTopK());
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
        if (config.getExtraBody() != null && !config.getExtraBody().isEmpty()) {
            builder.extraBody(config.getExtraBody());
        }
        return builder.build();
    }

    private long readTimeoutMs(ConfigExtAttrsDTO config) {
        Long timeoutMs = config != null ? config.getTimeoutMs() : null;
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : 60_000L;
    }
}
