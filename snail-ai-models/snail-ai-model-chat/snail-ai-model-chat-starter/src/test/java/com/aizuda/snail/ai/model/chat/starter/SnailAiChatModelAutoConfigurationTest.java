package com.aizuda.snail.ai.model.chat.starter;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.chat.ChatModelAdapter;
import com.aizuda.snail.ai.model.chat.ChatModelRuntime;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.chat.openai.responses.OpenAiResponsesChatModelAdapter;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.responses.OpenAiResponsesChatModel;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SnailAiChatModelAutoConfiguration} adapter registration tests.
 *
 * @author opensnail
 * @since 2026-11-14
 */
class SnailAiChatModelAutoConfigurationTest {

    @Test
    void shouldDiscoverResponsesAdapterWithoutChangingDefault() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(SnailAiChatModelAutoConfiguration.class);
            context.refresh();

            Map<String, ChatModelAdapter> adapters = context.getBeansOfType(ChatModelAdapter.class);
            assertEquals(2, adapters.size());
            assertTrue(adapters.values().stream()
                    .anyMatch(adapter -> OpenAiResponsesChatModelAdapter.ADAPTER_KEY.equals(adapter.adapterKey())));

            List<ModelAdapterDescriptor> descriptors = context.getBeansOfType(ModelAdapterDescriptor.class)
                    .values()
                    .stream()
                    .toList();
            assertEquals(2, descriptors.size());
            assertTrue(descriptors.stream()
                    .anyMatch(descriptor -> OpenAiResponsesChatModelAdapter.ADAPTER_KEY
                            .equals(descriptor.adapterKey())));
            assertEquals(ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER,
                    ModelAdapterDefaults.resolve(null, ModelAdapterDefaults.CHAT_MODEL_TYPE));

            ConfigExtAttrsDTO config = new ConfigExtAttrsDTO();
            config.setMaxRetries(0);
            ChatModelSpec spec = new ChatModelSpec(
                    OpenAiResponsesChatModelAdapter.ADAPTER_KEY,
                    "openai",
                    "http://127.0.0.1:9999/v1",
                    "test-api-key",
                    "gpt-test",
                    config);
            assertInstanceOf(OpenAiResponsesChatModel.class,
                    context.getBean(ChatModelRuntime.class).build(spec));
        }
    }
}
