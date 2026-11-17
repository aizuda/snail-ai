package com.aizuda.snail.ai.model.chat.openai.responses;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.HostedToolConfigDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.responses.HostedTool;
import org.springframework.ai.openai.responses.OpenAiResponsesChatModel;
import org.springframework.ai.openai.responses.OpenAiResponsesChatOptions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenAiResponsesChatModelAdapter} 配置映射测试。
 *
 * @author opensnail
 * @since 2026-11-14
 */
class OpenAiResponsesChatModelAdapterTest {

    private final OpenAiResponsesChatModelAdapter adapter = new OpenAiResponsesChatModelAdapter();

    @Test
    void shouldMapResponsesOptionsAndHostedTools() {
        ConfigExtAttrsDTO config = responsesConfig();

        ChatModel chatModel = adapter.create(spec(config));

        OpenAiResponsesChatModel responsesModel = assertInstanceOf(OpenAiResponsesChatModel.class, chatModel);
        OpenAiResponsesChatOptions options = responsesModel.getOptions();
        assertEquals("gpt-test", options.getModel());
        assertEquals(4096, options.getMaxOutputTokens());
        assertEquals("high", options.getReasoningEffort());
        assertEquals("detailed", options.getReasoningSummary());
        assertEquals("low", options.getVerbosity());
        assertEquals("auto", options.getToolChoice());
        assertEquals(Boolean.TRUE, options.getParallelToolCalls());
        assertEquals(6, options.getMaxToolCalls());
        assertEquals(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT, options.getResponseFormat().getType());
        assertEquals("value", options.getExtraBody().get("custom_parameter"));
        assertNull(options.getTopK());
        assertNull(options.getFrequencyPenalty());
        assertNull(options.getPresencePenalty());

        List<HostedTool> hostedTools = options.getHostedTools();
        assertEquals(3, hostedTools.size());
        assertInstanceOf(HostedTool.CodeInterpreter.class, hostedTools.get(0));
        assertInstanceOf(HostedTool.ImageGeneration.class, hostedTools.get(1));
        assertInstanceOf(HostedTool.Raw.class, hostedTools.get(2));
    }

    @Test
    void shouldRoundTripHostedToolConfiguration() {
        ConfigExtAttrsDTO original = responsesConfig();

        ConfigExtAttrsDTO restored = JsonUtil.parseObject(JsonUtil.toJsonString(original), ConfigExtAttrsDTO.class);

        assertEquals("HIGH", restored.getReasoningEffort());
        assertEquals(3, restored.getHostedTools().size());
        assertEquals("cntr_123", restored.getHostedTools().get(0).getContainerId());
        assertEquals("computer_preview", restored.getHostedTools().get(2).getTool().get("type"));
    }

    @Test
    void shouldRejectProviderWebSearchFileSearchAndHostedMcp() {
        assertRejected(hostedTool("web_search"));
        assertRejected(hostedTool("file_search"));

        HostedToolConfigDTO raw = hostedTool("raw");
        raw.setTool(Map.of("type", "mcp", "server_url", "https://mcp.example.com"));
        assertRejected(raw);

        ConfigExtAttrsDTO extraBody = new ConfigExtAttrsDTO();
        extraBody.setExtraBody(Map.of("tools", List.of(
                Map.of("type", "web_search_preview"),
                Map.of("type", "file_search"))));
        ModelAdapterException exception = assertThrows(
                ModelAdapterException.class, () -> adapter.create(spec(extraBody)));
        assertTrue(exception.getMessage().contains("is not supported"));
    }

    @Test
    void shouldKeepOpenAiCompatibleAsDefaultChatAdapter() {
        assertEquals(ModelAdapterDefaults.OPENAI_COMPATIBLE_ADAPTER,
                ModelAdapterDefaults.resolve(null, ModelAdapterDefaults.CHAT_MODEL_TYPE));
        assertEquals(OpenAiResponsesChatModelAdapter.ADAPTER_KEY, adapter.adapterKey());
    }

    private ConfigExtAttrsDTO responsesConfig() {
        ConfigExtAttrsDTO config = new ConfigExtAttrsDTO();
        config.setTimeoutMs(180_000L);
        config.setMaxRetries(3);
        config.setTemperature(0.4);
        config.setTopP(0.9);
        config.setTopK(20);
        config.setMaxTokens(4096);
        config.setFrequencyPenalty(0.2);
        config.setPresencePenalty(0.1);
        config.setResponseFormat("json_object");
        config.setReasoningEffort("HIGH");
        config.setReasoningSummary("detailed");
        config.setVerbosity("low");
        config.setToolChoice("auto");
        config.setParallelToolCalls(true);
        config.setMaxToolCalls(6);
        config.setExtraBody(Map.of("custom_parameter", "value"));
        config.setHostedTools(List.of(codeInterpreter(), imageGeneration(), rawTool()));
        return config;
    }

    private void assertRejected(HostedToolConfigDTO tool) {
        ConfigExtAttrsDTO config = new ConfigExtAttrsDTO();
        config.setHostedTools(List.of(tool));
        ModelAdapterException exception = assertThrows(ModelAdapterException.class, () -> adapter.create(spec(config)));
        assertTrue(exception.getMessage().contains("is not supported"));
    }

    private HostedToolConfigDTO codeInterpreter() {
        HostedToolConfigDTO config = hostedTool("code_interpreter");
        config.setContainerId("cntr_123");
        return config;
    }

    private HostedToolConfigDTO imageGeneration() {
        HostedToolConfigDTO config = hostedTool("image_generation");
        config.setModel("gpt-image-1");
        config.setSize("1024x1024");
        return config;
    }

    private HostedToolConfigDTO rawTool() {
        HostedToolConfigDTO config = hostedTool("raw");
        config.setTool(Map.of("type", "computer_preview"));
        return config;
    }

    private HostedToolConfigDTO hostedTool(String type) {
        HostedToolConfigDTO config = new HostedToolConfigDTO();
        config.setType(type);
        return config;
    }

    private ChatModelSpec spec(ConfigExtAttrsDTO config) {
        return new ChatModelSpec(
                OpenAiResponsesChatModelAdapter.ADAPTER_KEY,
                "openai",
                "http://localhost:9999/v1",
                "test-api-key",
                "gpt-test",
                config);
    }
}
