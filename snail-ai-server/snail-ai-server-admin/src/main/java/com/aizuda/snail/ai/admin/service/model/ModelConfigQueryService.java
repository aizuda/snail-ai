package com.aizuda.snail.ai.admin.service.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigQueryVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.admin.vo.model.ModelAdapterOptionVO;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.common.util.StreamUtils;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelAdapterDescriptor;
import com.aizuda.snail.ai.model.common.ModelCapability;
import com.aizuda.snail.ai.model.enums.ModelScopeEnum;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模型配置读侧查询组件
 *
 * <p>从 {@link AiModelConfigService} 提取的纯查询逻辑（单查 / 分页 / 按类型 / 默认模型 / 个人模型 / 适配器列表），
 * 与写操作分离后门面只承担事务性写编排。查询均为只读操作，不开启事务，与拆分前行为一致。
 * VO 组装（含 API Key 解密回显）收敛在本类的 {@link #convertToVO}，保证所有查询出口的脱敏口径统一。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelConfigQueryService {

    private final AiModelConfigMapper configMapper;
    private final AiModelProviderMapper providerMapper;
    private final ModelConfigValidator validator;
    private final ModelCredentialCipher credentialCipher;
    private final List<ModelAdapterDescriptor> modelAdapterDescriptors;

    /**
     * 获取单个模型配置
     *
     * @param modelId 模型配置 ID
     * @return 模型配置 VO；不存在时返回 null
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
     *
     * @param queryVO 分页与过滤条件
     * @return 模型配置分页结果（含提供商名称回填）
     */
    public PageResult<List<AiModelConfigVO>> listModelConfigs(AiModelConfigQueryVO queryVO) {
        PageDTO<AiModelConfigPO> pageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
        log.debug("查询模型配置: page={}, size={}, providerKey={}, modelType={}, scope={}",
                queryVO.getPage(), queryVO.getSize(), queryVO.getProviderKey(), queryVO.getModelType(), queryVO.getScope());

        LambdaQueryWrapper<AiModelConfigPO> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(queryVO.getProviderKey())) {
            AiModelProviderPO provider = providerMapper.selectOne(
                    new LambdaQueryWrapper<AiModelProviderPO>()
                            .eq(AiModelProviderPO::getProviderKey, queryVO.getProviderKey()));
            if (provider != null) {
                wrapper.eq(AiModelConfigPO::getProviderId, provider.getId());
            }
        }

        wrapper.eq(StrUtil.isNotBlank(queryVO.getModelType()), AiModelConfigPO::getModelType, queryVO.getModelType())
                .eq(StrUtil.isNotBlank(queryVO.getScope()), AiModelConfigPO::getScope, queryVO.getScope())
                .between(ObjUtil.isNotNull(queryVO.getStartDt()) && ObjUtil.isNotNull(queryVO.getEndDt()),
                        AiModelConfigPO::getCreatedDt, queryVO.getStartDt(), queryVO.getEndDt())
                .orderByDesc(AiModelConfigPO::getIsDefault)
                .orderByAsc(AiModelConfigPO::getCreatedDt);

        Page<AiModelConfigPO> page = configMapper.selectPage(pageDTO, wrapper);

        List<Long> providerIds = page.getRecords().stream().map(AiModelConfigPO::getProviderId).toList();
        if (CollUtil.isEmpty(providerIds)) {
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
     *
     * @param modelType 模型类型（chat/embedding/reranker 等）
     * @return 该类型下启用的模型列表
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
     *
     * @param provider  提供商唯一标识
     * @param modelType 模型类型
     * @return 匹配的启用模型列表
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
     *
     * @return 全局默认且启用的模型 VO；无则返回 null
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
     *
     * @param modelType 模型类型
     * @return 该类型默认且启用的模型 VO；无则返回 null
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
     * 获取用户的个人模型配置
     *
     * @param userId 用户 ID（模型 ownerId）
     * @return 该用户启用的个人范围模型列表
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
     * 获取模型类型支持的适配器列表：容器内无注册的适配器描述符时回退到内置默认值，
     * 保证前端下拉框在最小部署形态下也有可选项。
     *
     * @param modelType 模型类型
     * @return 该类型可用的适配器选项列表（含默认标记）
     */
    public List<ModelAdapterOptionVO> listModelAdapters(String modelType) {
        ModelCapability capability = validator.resolveCapability(modelType);
        if (capability == null) {
            return List.of();
        }
        String defaultAdapterKey = validator.resolveAdapterKey(null, modelType);
        List<ModelAdapterDescriptor> descriptors = CollUtil.isEmpty(modelAdapterDescriptors)
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
     * 转换为VO (隐藏敏感信息)
     */
    private AiModelConfigVO convertToVO(AiModelConfigPO po) {

        return AiModelConfigVO.builder()
                .id(po.getId())
                .providerId(po.getProviderId())
                .modelName(po.getModelName())
                .modelKey(po.getModelKey())
                .modelType(po.getModelType())
                .adapterKey(validator.resolveAdapterKey(po.getAdapterKey(), po.getModelType()))
                .description(po.getDescription())
                .apiKey(credentialCipher.decrypt(po.getApiKey()))
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
}
