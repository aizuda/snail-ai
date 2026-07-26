package com.aizuda.snail.ai.model.chat.openai;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.chat.ChatModelAdapter;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 兼容对话模型适配器，基于 Spring AI {@code OpenAiChatModel} 按规格与扩展参数构建对话模型。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
public class OpenAiCompatibleChatModelAdapter implements ChatModelAdapter, Ordered {

    /** 对话调用默认超时（毫秒），大模型长输出场景需要比普通 HTTP 更宽裕的读超时 */
    private static final long DEFAULT_TIMEOUT_MS = 300_000L;

    @Override
    public String adapterKey() {
        return ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER;
    }

    @Override
    public ChatModel create(ChatModelSpec spec) {
        log.debug("Creating OpenAI-compatible ChatModel, provider={}, model={}",
                spec.providerKey(), spec.modelKey());
        return OpenAiChatModel.builder()
                .options(buildOptions(spec))
                .build();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 由规格与扩展配置组装对话选项，扩展配置中未设置的参数不下发（保留供应商默认值）
     *
     * @param spec 对话模型构建规格
     * @return 对话选项
     */
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
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }
}
