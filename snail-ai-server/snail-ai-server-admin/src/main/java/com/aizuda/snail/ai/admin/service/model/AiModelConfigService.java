package com.aizuda.snail.ai.admin.service.model;

import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.model.enums.ModelScopeEnum;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigRequestVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.admin.vo.model.ModelAdapterOptionVO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI模型配置管理服务（门面）
 *
 * <p>对外公开方法签名与拆分前完全一致，内部按职责委托同包协作类：
 * <ul>
 *     <li>{@link ModelConfigValidator}：提供商存在性、引用占用、可禁用等业务规则校验与适配器键规整；</li>
 *     <li>{@link ModelCredentialCipher}：API Key 加解密与"更新时空白不修改"语义；</li>
 *     <li>{@link ModelConfigQueryService}：读侧查询（单查 / 分页 / 默认模型 / 适配器列表）与 VO 组装；</li>
 *     <li>{@link ModelConnectivityTester}：模型连通性一键测试（由控制器直接调用，不经过本门面）。</li>
 * </ul>
 * 本类只保留写操作编排：事务注解均落在 public 入口方法上（不存在内部自调用事务方法的场景），
 * 事务边界与拆分前一致。
 *
 * 功能:
 * - 模型CRUD操作
 * - API Key加密存储
 * - 按类型查询和切换默认模型
 * - Redis缓存管理
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelConfigService {

    private final AiModelConfigMapper configMapper;
    private final ModelConfigValidator validator;
    private final ModelCredentialCipher credentialCipher;
    private final ModelConfigQueryService queryService;

    /**
     * 新增模型配置 (Admin权限)
     *
     * @param requestVO 模型配置请求参数
     * @return 新建模型配置的主键 ID
     */
    @Transactional
    public Long addModelConfig(AiModelConfigRequestVO requestVO) {
        log.info("添加模型配置: modelName={}, modelType={}, provider={}",
                requestVO.getModelName(), requestVO.getModelType(), requestVO.getProviderId());

        validator.ensureProviderExists(requestVO.getProviderId());
        validator.ensureApiKeyPresent(requestVO.getApiKey());
        String encryptedKey = credentialCipher.encrypt(requestVO.getApiKey());

        AiModelConfigPO config = AiModelConfigPO.builder()
                .providerId(requestVO.getProviderId())
                .modelName(requestVO.getModelName())
                .modelKey(requestVO.getModelKey())
                .modelType(requestVO.getModelType())
                .adapterKey(validator.resolveAdapterKey(requestVO.getAdapterKey(), requestVO.getModelType()))
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
     *
     * @param modelId   模型配置 ID
     * @param requestVO 模型配置更新参数（仅非空字段生效）
     * @return 更新是否成功
     */
    @Transactional
    public boolean updateModelConfig(Long modelId, AiModelConfigRequestVO requestVO) {
        log.info("更新模型配置: id={}", modelId);

        AiModelConfigPO existing = configMapper.selectById(modelId);
        validator.ensureConfigExists(existing, modelId);

        String apiKey = credentialCipher.resolveUpdateApiKey(requestVO.getApiKey(), existing.getApiKey());
        String modelType = requestVO.getModelType() != null ? requestVO.getModelType() : existing.getModelType();
        String adapterKey = validator.resolveUpdateAdapterKey(requestVO, existing, modelType);

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
     * 删除模型配置（仍被智能体或知识库引用时拒绝删除）
     *
     * @param modelId 模型配置 ID
     * @return 删除是否成功；不存在时返回 false
     */
    @Transactional
    public boolean deleteModelConfig(Long modelId) {
        log.info("删除模型配置: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        if (config == null) {
            return false;
        }

        validator.ensureNotReferenced(modelId);

        boolean success = configMapper.deleteById(modelId) > 0;
        if (success && config.getIsDefault()) {
            clearCacheByType(config.getModelType());
        }
        return success;
    }

    /**
     * 获取单个模型配置
     *
     * @param modelId 模型配置 ID
     * @return 模型配置 VO；不存在时返回 null
     */
    public AiModelConfigVO getModelConfig(Long modelId) {
        return queryService.getModelConfig(modelId);
    }

    /**
     * 分页查询模型配置
     * 支持按providerKey、modelType、scope过滤
     *
     * @param queryVO 分页与过滤条件
     * @return 模型配置分页结果（含提供商名称回填）
     */
    public PageResult<List<AiModelConfigVO>> listModelConfigs(AiModelConfigQueryVO queryVO) {
        return queryService.listModelConfigs(queryVO);
    }

    /**
     * 按模型类型查询启用的模型
     *
     * @param modelType 模型类型（chat/embedding/reranker 等）
     * @return 该类型下启用的模型列表
     */
    public List<AiModelConfigVO> getModelsByType(String modelType) {
        return queryService.getModelsByType(modelType);
    }

    /**
     * 按提供商和模型类型查询
     *
     * @param provider  提供商唯一标识
     * @param modelType 模型类型
     * @return 匹配的启用模型列表
     */
    public List<AiModelConfigVO> getModelsByProviderAndType(String provider, String modelType) {
        return queryService.getModelsByProviderAndType(provider, modelType);
    }

    /**
     * 获取全局默认模型
     *
     * @return 全局默认且启用的模型 VO；无则返回 null
     */
    public AiModelConfigVO getDefaultModel() {
        return queryService.getDefaultModel();
    }

    /**
     * 按类型获取默认模型
     *
     * @param modelType 模型类型
     * @return 该类型默认且启用的模型 VO；无则返回 null
     */
    public AiModelConfigVO getDefaultModelByType(String modelType) {
        return queryService.getDefaultModelByType(modelType);
    }

    /**
     * 切换默认模型
     *
     * @param modelId 目标模型配置 ID（需存在且启用）
     * @return 切换是否成功
     */
    @Transactional
    public boolean switchDefaultModel(Long modelId) {
        log.info("切换默认模型: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        validator.ensureSwitchable(config, modelId);

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
     *
     * @param userId 用户 ID（模型 ownerId）
     * @return 该用户启用的个人范围模型列表
     */
    public List<AiModelConfigVO> getPersonalModels(Long userId) {
        return queryService.getPersonalModels(userId);
    }

    /**
     * 获取模型类型支持的适配器列表
     *
     * @param modelType 模型类型
     * @return 该类型可用的适配器选项列表（含默认标记）
     */
    public List<ModelAdapterOptionVO> listModelAdapters(String modelType) {
        return queryService.listModelAdapters(modelType);
    }

    /**
     * 仅更新模型扩展配置（如自动写入 embedding 维度）
     *
     * @param modelId    模型配置 ID
     * @param configJson 扩展配置内容
     */
    @Transactional
    public void updateConfigJson(Long modelId, ConfigExtAttrsDTO configJson) {
        AiModelConfigPO po = configMapper.selectById(modelId);
        validator.ensureConfigExists(po, modelId);
        po.setConfigJson(JsonUtil.toJsonString(configJson));
        po.setUpdatedDt(LocalDateTime.now());
        configMapper.updateById(po);
        clearCacheByType(po.getModelType());
    }

    /**
     * 启用模型配置 (Admin)
     *
     * @param modelId 模型配置 ID
     * @return 启用是否成功；已启用时返回 true
     */
    @Transactional
    public boolean enableModelConfig(Long modelId) {
        log.info("启用模型配置: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        validator.ensureConfigExists(config, modelId);

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
     *
     * @param modelId 模型配置 ID
     * @return 禁用是否成功；已禁用时返回 true
     */
    @Transactional
    public boolean disableModelConfig(Long modelId) {
        log.info("禁用模型配置: id={}", modelId);

        AiModelConfigPO config = configMapper.selectById(modelId);
        validator.ensureConfigExists(config, modelId);

        if (!config.getIsEnabled()) {
            log.debug("模型配置已经是禁用状态: {}", modelId);
            return true;
        }

        validator.ensureCanDisable(config, modelId);

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

    // ==================== 私有方法 ====================

    /**
     * 清除特定类型的缓存
     */
    private void clearCacheByType(String modelType) {
        // 这里可以集成Redis缓存清除逻辑
        log.debug("清除缓存: modelType={}", modelType);
    }
}
