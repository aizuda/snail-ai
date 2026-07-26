package com.aizuda.snail.ai.model.handle;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.crypto.CryptoHelper;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置处理器，提供模型配置信息的查询（脱敏）、API Key 解密与按类型获取默认模型。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelConfigHandler implements VisionModelConfigResolver {
    private static final String VISION_CAPABILITY = "vision";

    private final AiModelConfigMapper configMapper;
    private final CryptoHelper cryptoHelper;
    private final AiModelProviderMapper providerMapper;

    /**
     * 获取模型配置信息 (用于DynamicModelCaller，返回ModelConfigInfo)
     * 返回脱敏的配置信息，不暴露完整API Key
     *
     * @param modelId 模型配置 ID
     * @return 模型配置信息（API Key 为加密态）；不存在时返回 null
     */
    public ModelConfigInfoDTO getConfigInfo(Long modelId) {
        return buildConfigInfoDTO(configMapper.selectById(modelId));
    }

    /**
     * 解密API Key
     *
     * @param encryptedApiKey 加密的 API Key
     * @return 解密后的明文 API Key
     */
    public String decryptApiKey(String encryptedApiKey) {
        return cryptoHelper.decrypt(encryptedApiKey);
    }

    /**
     * 按类型获取默认模型
     *
     * @param modelType 模型类型（chat / embedding / reranker 等）
     * @return 该类型下默认且启用的模型配置；无则返回 null
     */
    public ModelConfigInfoDTO getDefaultModelByType(String modelType) {

        List<Long> enabledProviderIds = getEnabledProviderIds();
        if (enabledProviderIds.isEmpty()) {
            log.warn("获取默认模型失败：无启用的模型提供商, modelType={}", modelType);
            return null;
        }

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getModelType, modelType)
                .eq(AiModelConfigPO::getIsDefault, true)
                .eq(AiModelConfigPO::getIsEnabled, true)
                .in(AiModelConfigPO::getProviderId, enabledProviderIds)
                .last("LIMIT 1");

        ModelConfigInfoDTO config = buildConfigInfoDTO(configMapper.selectOne(wrapper));
        if (config == null) {
            log.warn("该类型下无「默认且启用」的模型配置, modelType={}", modelType);
        }
        return config;
    }

    /**
     * 获取默认视觉对话模型：在启用的 chat 模型中筛选具备 vision 能力的配置，
     * 优先取标记为默认视觉模型的，其次按「是否默认对话模型、更新时间」排序取第一个
     *
     * @return 视觉对话模型配置；无可用视觉模型时返回 null
     */
    @Override
    public ModelConfigInfoDTO getDefaultVisionChatModel() {
        List<Long> enabledProviderIds = getEnabledProviderIds();
        if (enabledProviderIds.isEmpty()) {
            log.warn("获取默认视觉模型失败：无启用的模型提供商");
            return null;
        }

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getModelType, ModelAdapterDefaults.CHAT_MODEL_TYPE)
                .eq(AiModelConfigPO::getIsEnabled, true)
                .in(AiModelConfigPO::getProviderId, enabledProviderIds)
                .orderByDesc(AiModelConfigPO::getIsDefault)
                .orderByDesc(AiModelConfigPO::getUpdatedDt);

        return configMapper.selectList(wrapper)
                .stream()
                .filter(this::isVisionChatModel)
                .sorted((left, right) -> Boolean.compare(isDefaultVisionModel(right), isDefaultVisionModel(left)))
                .findFirst()
                .map(this::buildConfigInfoDTO)
                .orElse(null);
    }

    /** 将配置 PO 组装为业务 DTO：补充提供商信息、解析扩展 JSON，API Key 保持加密态不解密 */
    private ModelConfigInfoDTO buildConfigInfoDTO(AiModelConfigPO po) {
        if (po == null) {
            return null;
        }
        AiModelProviderPO provider = providerMapper.selectById(po.getProviderId());

        return ModelConfigInfoDTO.builder()
                .id(po.getId())
                .providerId(po.getProviderId())
                .providerName(provider != null ? provider.getProviderName() : null)
                .providerKey(provider != null ? provider.getProviderKey() : null)
                .modelName(po.getModelName())
                .modelKey(po.getModelKey())
                .modelType(po.getModelType())
                .adapterKey(ModelAdapterDefaults.resolve(po.getAdapterKey(), po.getModelType()))
                .encryptedApiKey(po.getApiKey())
                .description(po.getDescription())
                .apiEndpoint(po.getApiEndpoint())
                .configJson(JsonUtil.parseObject(
                        Optional.ofNullable(po.getConfigJson()).orElse("{}"), ConfigExtAttrsDTO.class))
                .scope(po.getScope())
                .isDefault(po.getIsDefault())
                .enabled(po.getIsEnabled())
                .updatedDt(po.getUpdatedDt())
                .build();
    }

    private boolean isVisionChatModel(AiModelConfigPO po) {
        ConfigExtAttrsDTO attrs = parseConfigJson(po);
        return attrs.getCapabilities() != null && attrs.getCapabilities().stream()
                .anyMatch(VISION_CAPABILITY::equalsIgnoreCase);
    }

    private boolean isDefaultVisionModel(AiModelConfigPO po) {
        return Boolean.TRUE.equals(parseConfigJson(po).getDefaultVisionModel());
    }

    private ConfigExtAttrsDTO parseConfigJson(AiModelConfigPO po) {
        if (po == null) {
            return new ConfigExtAttrsDTO();
        }
        return JsonUtil.parseObject(Optional.ofNullable(po.getConfigJson()).orElse("{}"), ConfigExtAttrsDTO.class);
    }


    private List<Long> getEnabledProviderIds() {
        return providerMapper.selectList(new LambdaQueryWrapper<AiModelProviderPO>()
                        .select(AiModelProviderPO::getId)
                        .eq(AiModelProviderPO::getIsEnabled, true))
                .stream()
                .map(AiModelProviderPO::getId)
                .toList();
    }
}
