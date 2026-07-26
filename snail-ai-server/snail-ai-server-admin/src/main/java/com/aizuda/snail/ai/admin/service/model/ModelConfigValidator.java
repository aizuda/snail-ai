package com.aizuda.snail.ai.admin.service.model;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigRequestVO;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.common.ModelCapability;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模型配置校验器
 *
 * <p>从 {@link AiModelConfigService} 提取的参数合法性校验与适配器键规整逻辑，
 * 集中管理"什么样的模型配置可以增 / 删 / 改 / 切换"的业务规则，便于单独测试与复用。
 * 校验方法自身不开启事务，在调用方事务内执行，保证与拆分前一致的事务边界。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelConfigValidator {

    private final AiModelProviderMapper providerMapper;
    private final AgentMapper agentMapper;
    private final RagMapper ragMapper;

    /**
     * 校验提供商必须存在：模型配置挂在提供商之下，允许写入悬空的 providerId 会导致后续查询回填提供商名称失败。
     *
     * @param providerId 提供商 ID
     */
    public void ensureProviderExists(Long providerId) {
        if (providerMapper.selectById(providerId) == null) {
            log.error("提供商不存在: {}", providerId);
            throw new SnailAiException("提供商不存在");
        }
    }

    /**
     * 校验新增时必须携带 API Key：新增场景没有旧密文可沿用，缺失会导致模型调用时鉴权失败。
     *
     * @param apiKey API Key 明文
     */
    public void ensureApiKeyPresent(String apiKey) {
        Assert.notBlank(apiKey, () -> new SnailAiException("apiKey is blank"));
    }

    /**
     * 校验模型配置必须存在：更新 / 启停等操作的前置条件，避免对不存在的记录做无效写入。
     *
     * @param config  已加载的模型配置（可能为 null）
     * @param modelId 模型配置 ID（仅用于日志定位）
     */
    public void ensureConfigExists(AiModelConfigPO config, Long modelId) {
        if (config == null) {
            log.error("模型配置不存在: {}", modelId);
            throw new SnailAiException("模型配置不存在");
        }
    }

    /**
     * 校验模型未被引用：仍被智能体或知识库使用的模型不允许删除，
     * 否则会话 / 向量检索会在运行期因模型缺失而失败。提示语中带引用数量，方便管理员定位。
     *
     * @param modelId 模型配置 ID
     */
    public void ensureNotReferenced(Long modelId) {
        long agentCnt = agentMapper.selectCount(
                new LambdaQueryWrapper<AgentPO>().eq(AgentPO::getChatModelId, modelId));
        long ragCnt = ragMapper.selectCount(
                new LambdaQueryWrapper<RagPO>()
                        .eq(RagPO::getEmbeddingModelId, modelId)
                        .or()
                        .eq(RagPO::getRerankModelId, modelId));
        if (agentCnt > 0 || ragCnt > 0) {
            StringBuilder sb = new StringBuilder("该模型仍被");
            if (agentCnt > 0) {
                sb.append(agentCnt).append(" 个智能体");
            }
            if (ragCnt > 0) {
                if (agentCnt > 0) {
                    sb.append("和 ");
                }
                sb.append(ragCnt).append(" 个知识库");
            }
            sb.append("使用，无法删除");
            log.warn("模型仍被引用无法删除: modelId={}, agentCnt={}, ragCnt={}", modelId, agentCnt, ragCnt);
            throw new SnailAiException(sb.toString());
        }
    }

    /**
     * 校验模型可被设为默认：不存在或已禁用的模型设为默认后，默认模型解析会拿到不可用配置。
     *
     * @param config  已加载的模型配置（可能为 null）
     * @param modelId 模型配置 ID（仅用于日志定位）
     */
    public void ensureSwitchable(AiModelConfigPO config, Long modelId) {
        if (config == null || !config.getIsEnabled()) {
            log.error("模型不存在或已禁用: {}", modelId);
            throw new SnailAiException("模型不存在或已禁用");
        }
    }

    /**
     * 校验模型可被禁用：默认模型是各类型的兜底选择，直接禁用会让依赖默认模型的功能全部不可用，
     * 必须先切换默认模型再禁用。
     *
     * @param config  已加载的模型配置
     * @param modelId 模型配置 ID（仅用于日志定位）
     */
    public void ensureCanDisable(AiModelConfigPO config, Long modelId) {
        if (config.getIsDefault()) {
            log.error("默认模型不允许禁用: {}", modelId);
            throw new SnailAiException("默认模型不允许禁用，请先切换默认模型");
        }
    }

    /**
     * 规整适配器键：为空时按模型类型回退到默认适配器，保证落库的 adapterKey 永远可被适配器工厂解析。
     *
     * @param adapterKey 请求中的适配器键（可能为空）
     * @param modelType  模型类型
     * @return 合法的适配器键
     */
    public String resolveAdapterKey(String adapterKey, String modelType) {
        return ModelAdapterDefaults.resolve(adapterKey, modelType);
    }

    /**
     * 更新场景下解析适配器键：显式传入的优先；未传入但模型类型发生变化时，
     * 旧适配器键可能与新类型不兼容，必须回退到新类型的默认适配器；类型未变则沿用旧值。
     *
     * @param requestVO 更新请求
     * @param existing  数据库中的既有配置
     * @param modelType 本次更新后生效的模型类型
     * @return 应落库的适配器键
     */
    public String resolveUpdateAdapterKey(AiModelConfigRequestVO requestVO, AiModelConfigPO existing, String modelType) {
        if (StrUtil.isNotBlank(requestVO.getAdapterKey())) {
            return resolveAdapterKey(requestVO.getAdapterKey(), modelType);
        }
        if (isModelTypeChanged(existing.getModelType(), modelType)) {
            return resolveAdapterKey(null, modelType);
        }
        return resolveAdapterKey(existing.getAdapterKey(), modelType);
    }

    /**
     * 将模型类型字符串映射为适配器能力枚举；未知类型返回 null 由调用方决定兜底行为（如返回空列表）。
     *
     * @param modelType 模型类型
     * @return 对应能力；无法识别时返回 null
     */
    public ModelCapability resolveCapability(String modelType) {
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

    private boolean isModelTypeChanged(String oldModelType, String newModelType) {
        if (StrUtil.isBlank(oldModelType)) {
            return StrUtil.isNotBlank(newModelType);
        }
        return !oldModelType.equalsIgnoreCase(newModelType);
    }
}
