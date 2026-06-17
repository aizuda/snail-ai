package com.aizuda.snail.ai.admin.service.model;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterDescriptor;
import com.aizuda.snail.ai.model.common.ModelCapability;
import com.aizuda.snail.ai.model.crypto.CryptoHelper;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.common.util.StreamUtils;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.enums.ModelScopeEnum;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigRequestVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.admin.vo.model.ModelAdapterOptionVO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI模型配置管理服务
 *
 * 功能:
 * - 模型CRUD操作
 * - API Key加密存储
 * - 按类型查询和切换默认模型
 * - Redis缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelConfigService {

    private final AiModelConfigMapper configMapper;
    private final AiModelProviderMapper providerMapper;
    private final CryptoHelper cryptoHelper;
    private final List<ModelAdapterDescriptor> modelAdapterDescriptors;

    /**
     * 新增模型配置 (Admin权限)
     */
    @Transactional
    public Long addModelConfig(AiModelConfigRequestVO requestVO) {
        log.info("添加模型配置: modelName={}, modelType={}, provider={}",
                requestVO.getModelName(), requestVO.getModelType(), requestVO.getProviderId());

        // 验证提供商是否存在
        if (!validateProviderExists(requestVO.getProviderId())) {
            log.error("提供商不存在: {}", requestVO.getProviderId());
            throw new SnailAiException("提供商不存在");
        }

        Assert.notBlank(requestVO.getApiKey(), () -> new SnailAiException("apiKey is blank"));
        // 加密API Key
        String encryptedKey = encryptApiKey(requestVO.getApiKey());

        AiModelConfigPO config = AiModelConfigPO.builder()
                .providerId(requestVO.getProviderId())
                .modelName(requestVO.getModelName())
                .modelKey(requestVO.getModelKey())
                .modelType(requestVO.getModelType())
                .adapterKey(resolveAdapterKey(requestVO.getAdapterKey(), requestVO.getModelType()))
                .description(requestVO.getDescription())
                .apiKey(encryptedKey)
                .apiEndpoint(requestVO.getApiEndpoint())
                .configJson(JsonUtil.toJsonString(requestVO.getConfigJson()))
                .ownerId(requestVO.getOwnerId())
                .scope(requestVO.getScope() != null ?
                        requestVO.getScope() :
                        ModelScopeEnum.GLOBAL.getValue())
                .isDefault(requestVO.getIsDefault() != null ?
                        requestVO.getIsDefault() :
                        false)
                .isEnabled(true)
                .createdDt(LocalDateTime.now())
                .updatedDt(LocalDateTime.now())
                .build();

        configMapper.insert(config);
        log.info("添加模型配置成功: id={}", config.getId());

        // 清除对应类型的缓存
        if (config.getIsDefault()) {
            clearCacheByType(config.getModelType());
        }

        return config.getId();
    }

    /**
     * 更新模型配置
     */
    @Transactional
    public boolean updateModelConfig(Long modelId, AiModelConfigRequestVO requestVO) {
        log.info("更新模型配置: id={}", modelId);

        AiModelConfigPO existing = configMapper.selectById(modelId);
        if (existing == null) {
            log.error("模型不存在: {}", modelId);
            throw new SnailAiException("模型配置不存在");
        }

        String apiKey = resolveUpdateApiKey(requestVO.getApiKey(), existing.getApiKey());
        String modelType = requestVO.getModelType() != null ? requestVO.getModelType() : existing.getModelType();
        String adapterKey = resolveUpdateAdapterKey(requestVO, existing, modelType);

        AiModelConfigPO config = AiModelConfigPO.builder()
                .id(modelId)
                .providerId(requestVO.getProviderId() != null ? requestVO.getProviderId() : existing.getProviderId())
                .modelName(requestVO.getModelName() != null ? requestVO.getModelName() : existing.getModelName())
                .modelKey(requestVO.getModelKey() != null ? requestVO.getModelKey() : existing.getModelKey())
                .modelType(modelType)
                .adapterKey(adapterKey)
                .description(requestVO.getDescription() != null ? requestVO.getDescription() : existing.getDescription())
                .apiKey(apiKey)
                .apiEndpoint(requestVO.getApiEndpoint() != null ? requestVO.getApiEndpoint() : existing.getApiEndpoint())
                .configJson(JsonUtil.toJsonString(requestVO.getConfigJson()))
                .isDefault(requestVO.getIsDefault() != null ? requestVO.getIsDefault() : existing.getIsDefault())
                .updatedDt(LocalDateTime.now())
                .build();

        boolean success = configMapper.updateById(config) > 0;
        if (success) {
            clearCacheByType(config.getModelType());
        }
        return success;
    }

    /**
     * 删除模型配置
     */
    @Transactional
    public boolean deleteModelConfig(Long modelId) {
        log.info("删除模型配置: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        if (config == null) {
            return false;
        }

        boolean success = configMapper.deleteById(modelId) > 0;
        if (success && config.getIsDefault()) {
            clearCacheByType(config.getModelType());
        }
        return success;
    }

    /**
     * 获取单个模型配置
     */
    public AiModelConfigVO getModelConfig(Long modelId) {
        AiModelConfigPO po = configMapper.selectById(modelId);
        if (po == null) {
            return null;
        }
        return convertToVO(po);
    }

    /**
     * 分页查询模型配置
     * 支持按providerKey、modelType、scope过滤
     */
    public PageResult<List<AiModelConfigVO>> listModelConfigs(AiModelConfigQueryVO queryVO) {
        PageDTO<AiModelConfigPO> pageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
        log.debug("查询模型配置: page={}, size={}, providerKey={}, modelType={}, scope={}",
                queryVO.getPage(), queryVO.getSize(), queryVO.getProviderKey(), queryVO.getModelType(), queryVO.getScope());

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryVO.getProviderKey())) {
            AiModelProviderPO provider = providerMapper.selectOne(
                    new LambdaQueryWrapper<AiModelProviderPO>()
                            .eq(AiModelProviderPO::getProviderKey, queryVO.getProviderKey()));
            if (provider != null) {
                wrapper.eq(AiModelConfigPO::getProviderId, provider.getId());
            }
        }

        wrapper.eq(StringUtils.hasText(queryVO.getModelType()), AiModelConfigPO::getModelType, queryVO.getModelType())
                .eq(StringUtils.hasText(queryVO.getScope()), AiModelConfigPO::getScope, queryVO.getScope())
                .between(ObjUtil.isNotNull(queryVO.getStartDt()) && ObjUtil.isNotNull(queryVO.getEndDt()),
                        AiModelConfigPO::getCreatedDt, queryVO.getStartDt(), queryVO.getEndDt())
                .orderByDesc(AiModelConfigPO::getIsDefault)
                .orderByAsc(AiModelConfigPO::getCreatedDt);

        Page<AiModelConfigPO> page = configMapper.selectPage(pageDTO, wrapper);

        List<Long> providerIds = page.getRecords().stream().map(AiModelConfigPO::getProviderId).toList();
        if (CollectionUtils.isEmpty(providerIds)) {
            return new PageResult<>();
        }
        List<AiModelProviderPO> aiModelProviderPOS = providerMapper.selectByIds(providerIds);
        Map<Long, AiModelProviderPO> providerPOMap = StreamUtils.toIdentityMap(aiModelProviderPOS, AiModelProviderPO::getId);

        List<AiModelConfigVO> records = page.convert(this::convertToVO).getRecords();
        for (AiModelConfigVO record : records) {
            AiModelProviderPO aiModelProviderPO = providerPOMap.get(record.getProviderId());
            if (aiModelProviderPO != null) {
                record.setProviderName(aiModelProviderPO.getProviderName());
            }
        }

        return new PageResult<>(pageDTO, records);
    }

    /**
     * 按模型类型查询启用的模型
     */
    public List<AiModelConfigVO> getModelsByType(String modelType) {
        log.debug("按类型查询模型: modelType={}", modelType);

        List<Long> enabledProviderIds = getEnabledProviderIds();
        if (enabledProviderIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getModelType, modelType)
                .eq(AiModelConfigPO::getIsEnabled, true)
                .in(AiModelConfigPO::getProviderId, enabledProviderIds)
                .orderByDesc(AiModelConfigPO::getCreatedDt);

        List<AiModelConfigPO> poList = configMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 按提供商和模型类型查询
     */
    public List<AiModelConfigVO> getModelsByProviderAndType(String provider, String modelType) {
        log.debug("按提供商和类型查询模型: provider={}, modelType={}", provider, modelType);

        AiModelProviderPO providerPO = providerMapper.selectOne(
                new LambdaQueryWrapper<AiModelProviderPO>()
                        .eq(AiModelProviderPO::getProviderKey, provider)
                        .eq(AiModelProviderPO::getIsEnabled, true));
        if (providerPO == null) {
            return List.of();
        }

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getProviderId, providerPO.getId())
                .eq(AiModelConfigPO::getModelType, modelType)
                .eq(AiModelConfigPO::getIsEnabled, true)
                .orderByDesc(AiModelConfigPO::getCreatedDt);

        List<AiModelConfigPO> poList = configMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取全局默认模型
     */
    public AiModelConfigVO getDefaultModel() {
        log.debug("获取全局默认模型");

        List<Long> enabledProviderIds = getEnabledProviderIds();
        if (enabledProviderIds.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getIsDefault, true)
                .eq(AiModelConfigPO::getScope, ModelScopeEnum.GLOBAL.getValue())
                .eq(AiModelConfigPO::getIsEnabled, true)
                .in(AiModelConfigPO::getProviderId, enabledProviderIds)
                .last("LIMIT 1");

        AiModelConfigPO po = configMapper.selectOne(wrapper);
        return po != null ? convertToVO(po) : null;
    }

    /**
     * 按类型获取默认模型
     */
    public AiModelConfigVO getDefaultModelByType(String modelType) {
        log.debug("按类型获取默认模型: modelType={}", modelType);

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

        AiModelConfigPO po = configMapper.selectOne(wrapper);
        return po != null ? convertToVO(po) : null;
    }

    /**
     * 切换默认模型
     */
    @Transactional
    public boolean switchDefaultModel(Long modelId) {
        log.info("切换默认模型: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        if (config == null || !config.getIsEnabled()) {
            log.error("模型不存在或已禁用: {}", modelId);
            throw new SnailAiException("模型不存在或已禁用");
        }

        // 清除该类型的其他默认标记
        LambdaUpdateWrapper<AiModelConfigPO> updateWrapper = new LambdaUpdateWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getModelType, config.getModelType())
                .set(AiModelConfigPO::getIsDefault, false);
        configMapper.update(null, updateWrapper);

        // 设置该模型为默认
        config.setIsDefault(true);
        config.setUpdatedDt(LocalDateTime.now());
        boolean success = configMapper.updateById(config) > 0;

        if (success) {
            clearCacheByType(config.getModelType());
        }

        return success;
    }

    /**
     * 获取用户的个人模型配置
     */
    public List<AiModelConfigVO> getPersonalModels(Long userId) {
        log.debug("获取用户的个人模型配置: userId={}", userId);

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getOwnerId, userId)
                .eq(AiModelConfigPO::getScope, ModelScopeEnum.PERSONAL.getValue())
                .eq(AiModelConfigPO::getIsEnabled, true)
                .orderByDesc(AiModelConfigPO::getCreatedDt);

        List<AiModelConfigPO> poList = configMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取模型类型支持的适配器列表
     */
    public List<ModelAdapterOptionVO> listModelAdapters(String modelType) {
        ModelCapability capability = resolveCapability(modelType);
        if (capability == null) {
            return List.of();
        }
        String defaultAdapterKey = resolveAdapterKey(null, modelType);
        List<ModelAdapterDescriptor> descriptors = CollectionUtils.isEmpty(modelAdapterDescriptors)
                ? ModelAdapterDefaults.defaultDescriptors()
                : modelAdapterDescriptors;
        return descriptors.stream()
                .filter(ModelAdapterDescriptor::enabled)
                .filter(descriptor -> capability == descriptor.capability())
                .sorted(Comparator.comparingInt(ModelAdapterDescriptor::order)
                        .thenComparing(ModelAdapterDescriptor::adapterKey))
                .map(descriptor -> ModelAdapterOptionVO.builder()
                        .adapterKey(descriptor.adapterKey())
                        .name(descriptor.name())
                        .modelType(modelType)
                        .isDefault(defaultAdapterKey.equals(descriptor.adapterKey()))
                        .build())
                .toList();
    }

    // ==================== 私有方法 ====================

    /**
     * 加密API Key
     */
    private String encryptApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        return cryptoHelper.encrypt(apiKey);
    }

    /**
     * 验证提供商是否存在
     */
    private boolean validateProviderExists(Long providerId) {
        return providerMapper.selectById(providerId) != null;
    }

    private List<Long> getEnabledProviderIds() {
        return providerMapper.selectList(
                new LambdaQueryWrapper<AiModelProviderPO>()
                        .eq(AiModelProviderPO::getIsEnabled, true)
                        .select(AiModelProviderPO::getId))
                .stream()
                .map(AiModelProviderPO::getId)
                .toList();
    }

    /**
     * 清除特定类型的缓存
     */
    private void clearCacheByType(String modelType) {
        // 这里可以集成Redis缓存清除逻辑
        log.debug("清除缓存: modelType={}", modelType);
    }

    private String resolveUpdateAdapterKey(AiModelConfigRequestVO requestVO, AiModelConfigPO existing, String modelType) {
        if (StringUtils.hasText(requestVO.getAdapterKey())) {
            return resolveAdapterKey(requestVO.getAdapterKey(), modelType);
        }
        if (isModelTypeChanged(existing.getModelType(), modelType)) {
            return resolveAdapterKey(null, modelType);
        }
        return resolveAdapterKey(existing.getAdapterKey(), modelType);
    }

    private String resolveAdapterKey(String adapterKey, String modelType) {
        return ModelAdapterDefaults.resolve(adapterKey, modelType);
    }

    private boolean isModelTypeChanged(String oldModelType, String newModelType) {
        if (!StringUtils.hasText(oldModelType)) {
            return StringUtils.hasText(newModelType);
        }
        return !oldModelType.equalsIgnoreCase(newModelType);
    }

    private String resolveUpdateApiKey(String apiKey, String existingApiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return existingApiKey;
        }
        return encryptApiKey(apiKey);
    }

    private ModelCapability resolveCapability(String modelType) {
        if (ModelAdapterDefaults.CHAT_MODEL_TYPE.equalsIgnoreCase(modelType)) {
            return ModelCapability.CHAT;
        }
        if (ModelAdapterDefaults.EMBEDDING_MODEL_TYPE.equalsIgnoreCase(modelType)) {
            return ModelCapability.EMBEDDING;
        }
        if (ModelAdapterDefaults.RERANKER_MODEL_TYPE.equalsIgnoreCase(modelType)) {
            return ModelCapability.RERANKER;
        }
        return null;
    }

    /**
     * 转换为VO (隐藏敏感信息)
     */
    private AiModelConfigVO convertToVO(AiModelConfigPO po) {

        return AiModelConfigVO.builder()
                .id(po.getId())
                .providerId(po.getProviderId())
                .modelName(po.getModelName())
                .modelKey(po.getModelKey())
                .modelType(po.getModelType())
                .adapterKey(resolveAdapterKey(po.getAdapterKey(), po.getModelType()))
                .description(po.getDescription())
                .apiKey(decryptApiKey(po.getApiKey()))
                .apiEndpoint(po.getApiEndpoint())
                .configJson(JsonUtil.parseObject(Optional.ofNullable(po.getConfigJson()).orElse("{}"), ConfigExtAttrsDTO.class))
                .ownerId(po.getOwnerId())
                .scope(po.getScope())
                .isDefault(po.getIsDefault())
                .isEnabled(po.getIsEnabled())
                .createdDt(po.getCreatedDt())
                .updatedDt(po.getUpdatedDt())
                .build();
    }

    private String decryptApiKey(String encryptedApiKey) {
        if (!StringUtils.hasText(encryptedApiKey)) {
            return "";
        }
        return cryptoHelper.decrypt(encryptedApiKey);
    }

    /**
     * 仅更新模型扩展配置（如自动写入 embedding 维度）
     */
    @Transactional
    public void updateConfigJson(Long modelId, ConfigExtAttrsDTO configJson) {
        AiModelConfigPO po = configMapper.selectById(modelId);
        if (po == null) {
            throw new SnailAiException("模型配置不存在");
        }
        po.setConfigJson(JsonUtil.toJsonString(configJson));
        po.setUpdatedDt(LocalDateTime.now());
        configMapper.updateById(po);
        clearCacheByType(po.getModelType());
    }

    /**
     * 启用模型配置 (Admin)
     */
    @Transactional
    public boolean enableModelConfig(Long modelId) {
        log.info("启用模型配置: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        if (config == null) {
            log.error("模型配置不存在: {}", modelId);
            throw new SnailAiException("模型配置不存在");
        }

        if (config.getIsEnabled()) {
            log.debug("模型配置已经是启用状态: {}", modelId);
            return true;
        }

        LambdaUpdateWrapper<AiModelConfigPO> updateWrapper = new LambdaUpdateWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getId, modelId)
                .set(AiModelConfigPO::getIsEnabled, true)
                .set(AiModelConfigPO::getUpdatedDt, LocalDateTime.now());

        boolean success = configMapper.update(null, updateWrapper) > 0;
        if (success) {
            clearCacheByType(config.getModelType());
            log.info("模型配置启用成功: id={}", modelId);
        }
        return success;
    }

    /**
     * 禁用模型配置 (Admin)
     */
    @Transactional
    public boolean disableModelConfig(Long modelId) {
        log.info("禁用模型配置: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        if (config == null) {
            log.error("模型配置不存在: {}", modelId);
            throw new SnailAiException("模型配置不存在");
        }

        if (!config.getIsEnabled()) {
            log.debug("模型配置已经是禁用状态: {}", modelId);
            return true;
        }

        // 如果是默认模型，不允许禁用
        if (config.getIsDefault()) {
            log.error("默认模型不允许禁用: {}", modelId);
            throw new SnailAiException("默认模型不允许禁用，请先切换默认模型");
        }

        LambdaUpdateWrapper<AiModelConfigPO> updateWrapper = new LambdaUpdateWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getId, modelId)
                .set(AiModelConfigPO::getIsEnabled, false)
                .set(AiModelConfigPO::getUpdatedDt, LocalDateTime.now());

        boolean success = configMapper.update(null, updateWrapper) > 0;
        if (success) {
            clearCacheByType(config.getModelType());
            log.info("模型配置禁用成功: id={}", modelId);
        }
        return success;
    }
}
