package com.aizuda.snail.ai.model.handle;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelConfigHandlerTest {

    private static final long PROVIDER_ID = 10L;
    private static final long TEXT_CHAT_MODEL_ID = 1L;
    private static final long VISION_CHAT_MODEL_ID = 2L;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AiModelConfigPO.class);
        TableInfoHelper.initTableInfo(assistant, AiModelProviderPO.class);
    }

    @Test
    void getDefaultVisionChatModelPrefersVisionFallbackModelOverDefaultTextChatModel() {
        ModelConfigHandler handler = new ModelConfigHandler(
                configMapper(List.of(defaultTextChatModel(), defaultVisionChatModel())),
                null,
                providerMapper());

        ModelConfigInfoDTO result = handler.getDefaultVisionChatModel();

        assertEquals(VISION_CHAT_MODEL_ID, result.getId());
    }

    @Test
    void getDefaultVisionChatModelReturnsNullWhenNoEnabledVisionChatModelExists() {
        ModelConfigHandler handler = new ModelConfigHandler(
                configMapper(List.of(defaultTextChatModel())),
                null,
                providerMapper());

        assertNull(handler.getDefaultVisionChatModel());
    }

    private AiModelConfigPO defaultTextChatModel() {
        return AiModelConfigPO.builder()
                .id(TEXT_CHAT_MODEL_ID)
                .providerId(PROVIDER_ID)
                .modelName("text-chat")
                .modelKey("text-chat")
                .modelType(ModelAdapterDefaults.CHAT_MODEL_TYPE)
                .configJson("{}")
                .isDefault(true)
                .isEnabled(true)
                .build();
    }

    private AiModelConfigPO defaultVisionChatModel() {
        ConfigExtAttrsDTO attrs = new ConfigExtAttrsDTO();
        attrs.setCapabilities(List.of("vision"));
        attrs.setDefaultVisionModel(true);
        return AiModelConfigPO.builder()
                .id(VISION_CHAT_MODEL_ID)
                .providerId(PROVIDER_ID)
                .modelName("vision-chat")
                .modelKey("vision-chat")
                .modelType(ModelAdapterDefaults.CHAT_MODEL_TYPE)
                .configJson(JsonUtil.toJsonString(attrs))
                .isDefault(false)
                .isEnabled(true)
                .build();
    }

    private AiModelConfigMapper configMapper(List<AiModelConfigPO> models) {
        return (AiModelConfigMapper) Proxy.newProxyInstance(
                AiModelConfigMapper.class.getClassLoader(),
                new Class<?>[]{AiModelConfigMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return models;
                    }
                    return null;
                });
    }

    private AiModelProviderMapper providerMapper() {
        AiModelProviderPO provider = AiModelProviderPO.builder()
                .id(PROVIDER_ID)
                .providerName("provider")
                .providerKey("provider")
                .isEnabled(true)
                .build();
        return (AiModelProviderMapper) Proxy.newProxyInstance(
                AiModelProviderMapper.class.getClassLoader(),
                new Class<?>[]{AiModelProviderMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return List.of(provider);
                    }
                    if ("selectById".equals(method.getName())) {
                        return provider;
                    }
                    return null;
                });
    }
}
