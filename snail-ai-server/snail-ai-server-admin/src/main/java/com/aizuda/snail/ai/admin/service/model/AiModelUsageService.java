package com.aizuda.snail.ai.admin.service.model;

import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatsQueryVO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelProviderMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelUsageStatMapper;
import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatVO;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelProviderPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelUsageStatPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI模型使用统计服务
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelUsageService implements com.aizuda.snail.ai.model.service.AiModelUsageService {

    private final AiModelUsageStatMapper usageStatMapper;
    private final AiModelConfigMapper configMapper;
    private final AiModelProviderMapper providerMapper;
    private final AgentConversationRecordMapper conversationRecordMapper;


    /**
     * 获取特定模型和用户的使用统计
     *
     * @param modelId 模型ID
     * @param userId  用户ID
     * @return 使用统计VO
     */
    public AiModelUsageStatVO getUsageStats(Long modelId, Long userId) {
        LambdaQueryWrapper<AiModelUsageStatPO> queryWrapper = new LambdaQueryWrapper<AiModelUsageStatPO>()
            .eq(AiModelUsageStatPO::getModelId, modelId)
            .eq(AiModelUsageStatPO::getUserId, userId);

        AiModelUsageStatPO stat = usageStatMapper.selectOne(queryWrapper);
        if (stat == null) {
            return null;
        }

        return enrichStatVO(convertToVO(stat));
    }

    /**
     * 获取用户的模型使用统计列表 (分页)
     * 仅包含该用户自己的统计
     *
     * @param userId  用户ID
     * @param queryVO 查询条件
     * @return 分页结果
     */
    public PageResult<List<AiModelUsageStatVO>> getUserModelStats(Long userId, AiModelUsageStatsQueryVO queryVO) {
        PageDTO<AiModelUsageStatPO> page = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
        LambdaQueryWrapper<AiModelUsageStatPO> queryWrapper = new LambdaQueryWrapper<AiModelUsageStatPO>()
            .eq(AiModelUsageStatPO::getUserId, userId)
            .orderByDesc(AiModelUsageStatPO::getUpdatedDt);

        PageDTO<AiModelUsageStatPO> result = usageStatMapper.selectPage(page, queryWrapper);
        List<AiModelUsageStatVO> records = result.getRecords().stream()
            .map(po -> enrichStatVO(convertToVO(po)))
            .collect(Collectors.toList());

        return new PageResult<>(result, records);
    }

    /**
     * 获取全局模型使用统计列表 (分页)
     * 仅Admin可以访问
     *
     * @param queryVO 查询条件
     * @return 分页结果
     */
    public PageResult<List<AiModelUsageStatVO>> getGlobalModelStats(AiModelUsageStatsQueryVO queryVO) {
        // 读时聚合：从对话消息记录按模型实时汇总真实用量，替代曾经无人写入、恒为空的 sai_model_usage_stat 表
        List<AiModelUsageStatPO> aggregated = conversationRecordMapper.aggregateModelUsageByModel();
        if (aggregated.isEmpty()) {
            return paginate(List.of(), queryVO.getPage(), queryVO.getSize());
        }

        // 批量预取模型配置与提供商，避免对每个模型逐条 selectById 造成 N+1
        Set<Long> modelIds = aggregated.stream()
            .map(AiModelUsageStatPO::getModelId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, AiModelConfigPO> configById = modelIds.isEmpty() ? Map.of()
            : configMapper.selectByIds(modelIds).stream()
                .collect(Collectors.toMap(AiModelConfigPO::getId, c -> c, (a, b) -> a));
        Set<Long> providerIds = configById.values().stream()
            .map(AiModelConfigPO::getProviderId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, AiModelProviderPO> providerById = providerIds.isEmpty() ? Map.of()
            : providerMapper.selectByIds(providerIds).stream()
                .collect(Collectors.toMap(AiModelProviderPO::getId, p -> p, (a, b) -> a));

        List<AiModelUsageStatVO> all = aggregated.stream()
            .map(this::convertToVO)
            .filter(Objects::nonNull)
            .map(vo -> {
                AiModelConfigPO config = configById.get(vo.getModelId());
                AiModelProviderPO provider = config != null ? providerById.get(config.getProviderId()) : null;
                applyModelInfo(vo, config, provider);
                return vo;
            })
            .sorted(Comparator.comparingLong(
                    (AiModelUsageStatVO vo) -> vo.getTotalTokensUsed() == null ? 0L : vo.getTotalTokensUsed())
                .reversed())
            .collect(Collectors.toList());

        return paginate(all, queryVO.getPage(), queryVO.getSize());
    }

    /** 内存分页：聚合结果集很小（模型数量级），直接切片返回。 */
    private PageResult<List<AiModelUsageStatVO>> paginate(List<AiModelUsageStatVO> all, int page, int size) {
        int safePage = page <= 0 ? 1 : page;
        int safeSize = size <= 0 ? 10 : size;
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        PageResult<List<AiModelUsageStatVO>> result = new PageResult<>();
        result.setPage((long) safePage);
        result.setSize((long) safeSize);
        result.setTotal((long) all.size());
        result.setData(all.subList(from, to));
        return result;
    }

    /**
     * 获取按模型类型统计的使用情况
     *
     * @param modelType 模型类型
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计列表
     */
    public List<AiModelUsageStatVO> getStatsByModelType(String modelType, LocalDate startDate, LocalDate endDate) {
        // 首先获取该类型的所有模型
        List<AiModelConfigPO> models = configMapper.selectList(
            new LambdaQueryWrapper<AiModelConfigPO>()
                .eq(AiModelConfigPO::getModelType, modelType)
        );

        if (models.isEmpty()) {
            return List.of();
        }

        // 获取每个模型的统计
        return models.stream()
            .flatMap(model -> {
                List<AiModelUsageStatPO> stats = usageStatMapper.selectList(
                    new LambdaQueryWrapper<AiModelUsageStatPO>()
                        .eq(AiModelUsageStatPO::getModelId, model.getId())
                        .between(AiModelUsageStatPO::getCreatedDt,
                            startDate.atStartOfDay(),
                            endDate.plusDays(1).atStartOfDay())
                );
                return stats.stream().map(po -> enrichStatVO(convertToVO(po)));
            })
            .collect(Collectors.toList());
    }

    /**
     * 丰富统计 VO 信息：补充模型名称、类型、提供商并计算成功率。
     * <p>
     * 单条路径（低频：单模型/用户维度查询）使用，内部各查一次配置与提供商；
     * 高频的 {@link #getGlobalModelStats} 走批量预取，不经过此方法，避免 N+1。
     *
     * @param vo 统计 VO，可能为 null
     * @return 回填后的 VO
     */
    private AiModelUsageStatVO enrichStatVO(AiModelUsageStatVO vo) {
        if (vo == null) {
            return null;
        }
        AiModelConfigPO config = configMapper.selectById(vo.getModelId());
        AiModelProviderPO provider = (config != null && config.getProviderId() != null)
            ? providerMapper.selectById(config.getProviderId()) : null;
        applyModelInfo(vo, config, provider);
        return vo;
    }

    /**
     * 用模型配置与提供商信息回填统计 VO（模型名/类型/提供商 ID 与名称，并计算成功率）。
     * 纯内存组装、不查库，批量与单条两条路径共用，避免重复逻辑。
     *
     * @param vo       统计 VO（非 null）
     * @param config   模型配置，可能为 null
     * @param provider 提供商，可能为 null
     */
    private void applyModelInfo(AiModelUsageStatVO vo, AiModelConfigPO config, AiModelProviderPO provider) {
        if (config != null) {
            vo.setModelName(config.getModelName());
            vo.setModelType(config.getModelType());
            vo.setProviderId(config.getProviderId());
        }
        if (provider != null) {
            vo.setProviderName(provider.getProviderName());
        }
        // 计算成功率 (0-100)
        if (vo.getTotalCalls() != null && vo.getTotalCalls() > 0 && vo.getSuccessCalls() != null) {
            double successRate = (vo.getSuccessCalls() * 100.0) / vo.getTotalCalls();
            vo.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
        } else {
            vo.setSuccessRate(0.0);
        }
    }

    /**
     * 转换PO为VO
     */
    private AiModelUsageStatVO convertToVO(AiModelUsageStatPO po) {
        if (po == null) {
            return null;
        }

        return AiModelUsageStatVO.builder()
            .id(po.getId())
            .modelId(po.getModelId())
            .userId(po.getUserId())
            .totalCalls(po.getTotalCalls())
            .successCalls(po.getSuccessCalls())
            .failedCalls(po.getFailedCalls())
            .totalTokensUsed(po.getTotalTokensUsed())
            .totalCost(po.getTotalCost())
            .avgResponseTime(po.getAvgResponseTime())
            .lastUsedDt(po.getLastUsedDt() != null ?
                po.getLastUsedDt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
            .createdDt(po.getCreatedDt() != null ?
                po.getCreatedDt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
            .updatedDt(po.getUpdatedDt() != null ?
                po.getUpdatedDt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
            .build();
    }

    // ========== 废弃的方法（保留接口签名用于兼容） ==========

    /**
     * @deprecated 已废弃。数据由观测性系统自动采集，无需手动调用。
     */
    @Deprecated
    @Override
    public void recordUsage(Long modelId, Long userId, Integer promptTokens, Integer completionTokens,
                            Long responseTime, Integer status, String errorMessage, String conversationId) {
        log.warn("recordUsage() is deprecated. Data is now collected by observability system automatically.");
    }
}
