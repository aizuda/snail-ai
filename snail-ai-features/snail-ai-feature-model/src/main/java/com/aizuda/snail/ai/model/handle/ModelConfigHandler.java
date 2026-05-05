package com.aizuda.snail.ai.model.handle;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.model.CryptoHelper;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ModelConfigHandler {
    private final AiModelConfigMapper configMapper;
    private final CryptoHelper cryptoHelper;
    private final AiModelProviderMapper providerMapper;

    /**
     * 获取模型配置信息 (用于DynamicModelCaller，返回ModelConfigInfo)
     * 返回脱敏的配置信息，不暴露完整API Key
     */
    public ModelConfigInfoDTO getConfigInfo(Long modelId) {
        return buildConfigInfoDTO(configMapper.selectById(modelId));
    }

    /**
     * 解密API Key
     */
    public String decryptApiKey(String encryptedApiKey) {
        return cryptoHelper.decrypt(encryptedApiKey);
    }

    /**
     * 按类型获取默认模型
     */
    public ModelConfigInfoDTO getDefaultModelByType(String modelType) {

        List<Long> enabledProviderIds = getEnabledProviderIds();
        if (enabledProviderIds.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getModelType, modelType)
                .eq(AiModelConfigPO::getIsDefault, true)
                .eq(AiModelConfigPO::getIsEnabled, true)
                .in(AiModelConfigPO::getProviderId, enabledProviderIds)
                .last("LIMIT 1");

        return buildConfigInfoDTO(configMapper.selectOne(wrapper));
    }

    private static ModelConfigInfoDTO buildConfigInfoDTO(AiModelConfigPO po) {
        if (po == null) {
            return null;
        }

        return ModelConfigInfoDTO.builder()
                .id(po.getId())
                .providerId(po.getProviderId())
                .modelName(po.getModelName())
                .modelKey(po.getModelKey())
                .modelType(po.getModelType())
                .encryptedApiKey(po.getApiKey())
                .description(po.getDescription())
                .apiEndpoint(po.getApiEndpoint())
                .configJson(JsonUtil.parseObject(Optional.ofNullable(po.getConfigJson()).orElse("{}"), ConfigExtAttrsDTO.class))
                .scope(po.getScope())
                .isDefault(po.getIsDefault())
                .enabled(po.getIsEnabled())
                .build();
    }


    private List<Long> getEnabledProviderIds() {
        return providerMapper.selectList(new LambdaQueryWrapper<AiModelProviderPO>()
                        .select(AiModelProviderPO::getId)
                        .eq(AiModelProviderPO::getIsEnabled, true)
                        .select(AiModelProviderPO::getId))
                .stream()
                .map(AiModelProviderPO::getId)
                .toList();
    }
}
