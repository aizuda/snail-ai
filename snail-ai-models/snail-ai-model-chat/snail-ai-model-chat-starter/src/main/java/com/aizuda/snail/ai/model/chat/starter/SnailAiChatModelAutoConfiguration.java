package com.aizuda.snail.ai.model.chat.starter;

import com.aizuda.snail.ai.model.chat.ChatModelAdapter;
import com.aizuda.snail.ai.model.chat.ChatModelRuntime;
import com.aizuda.snail.ai.model.chat.openai.OpenAiCompatibleChatModelAdapter;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterDescriptor;
import com.aizuda.snail.ai.model.common.ModelCapability;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 对话模型自动配置，装配对话模型运行时、OpenAI 兼容适配器及其描述符。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@AutoConfiguration
public class SnailAiChatModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChatModelRuntime chatModelRuntime(List<ChatModelAdapter> adapters) {
        return new ChatModelRuntime(adapters);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiCompatibleChatModelAdapter openAiCompatibleChatModelAdapter() {
        return new OpenAiCompatibleChatModelAdapter();
    }

    @Bean
    public ModelAdapterDescriptor openAiCompatibleChatAdapterDescriptor() {
        return ModelAdapterDescriptor.of(
                ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER,
                "OpenAI Compatible",
                ModelCapability.CHAT);
    }
}
