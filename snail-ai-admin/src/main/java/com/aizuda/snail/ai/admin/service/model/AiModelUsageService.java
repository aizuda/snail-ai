package com.aizuda.snail.ai.admin.service.model;

import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatsQueryVO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelUsageStatMapper;
import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatVO;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import com.aizuda.snail.ai.persistence.model.po.AiModelUsageStatPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI模型使用统计服务
 * 
 * @author opensnail
 * @since 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelUsageService implements com.aizuda.snail.ai.model.service.AiModelUsageService {

    private final AiModelUsageStatMapper usageStatMapper;
    private final AiModelConfigMapper configMapper;



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
        PageDTO<AiModelUsageStatPO> page = new PageDTO<>(queryVO.getPage(), queryVO.getSize());

        LambdaQueryWrapper<AiModelUsageStatPO> queryWrapper = new LambdaQueryWrapper<AiModelUsageStatPO>()
                .orderByDesc(AiModelUsageStatPO::getTotalCalls)
                .orderByDesc(AiModelUsageStatPO::getUpdatedDt);

        PageDTO<AiModelUsageStatPO> result = usageStatMapper.selectPage(page, queryWrapper);
        List<AiModelUsageStatVO> records = result.getRecords().stream()
                .map(po -> enrichStatVO(convertToVO(po)))
                .collect(Collectors.toList());

        return new PageResult<>(result, records);
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
     * 丰富统计VO信息
     * 添加模型名称、类型、提供商等信息
     */
    private AiModelUsageStatVO enrichStatVO(AiModelUsageStatVO vo) {
        if (vo == null) {
            return null;
        }

        // 获取模型配置信息
        AiModelConfigPO config = configMapper.selectById(vo.getModelId());
        if (config != null) {
            vo.setModelName(config.getModelName());
            vo.setModelType(config.getModelType());
            vo.setProviderId(config.getProviderId());

            // 计算成功率 (0-100)
            if (vo.getTotalCalls() != null && vo.getTotalCalls() > 0) {
                Double successRate = (vo.getSuccessCalls() * 100.0) / vo.getTotalCalls();
                vo.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
            } else {
                vo.setSuccessRate(0.0);
            }
        }

        return vo;
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

    // ========== 内部类 ==========

    /**
     * 模型-用户 组合键
     */
    private static class ModelUserKey {
        final String modelKey;
        final Long userId;

        ModelUserKey(String modelKey, Long userId) {
            this.modelKey = modelKey;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModelUserKey that = (ModelUserKey) o;
            return Objects.equals(modelKey, that.modelKey) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(modelKey, userId);
        }
    }

    /**
     * 使用量聚合
     */
    private static class UsageAggregation {
        long totalCalls = 0;
        long successCalls = 0;
        long failedCalls = 0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalResponseTime = 0;
        LocalDateTime lastUsedTime = null;
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
