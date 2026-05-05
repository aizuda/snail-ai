package com.aizuda.snail.ai.admin.vo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 模型使用统计 VO
 * 用于返回模型使用统计信息给前端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelUsageStatVO {

    /**
     * 统计记录ID
     */
    private Long id;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型类型
     */
    private String modelType;

    /**
     * 提供商ID
     */
    private Long providerId;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 成功调用次数
     */
    private Long successCalls;

    /**
     * 失败调用次数
     */
    private Long failedCalls;

    /**
     * 成功率 (0-100)
     */
    private Double successRate;

    /**
     * 总Token使用量
     */
    private Long totalTokensUsed;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 平均响应时间 (毫秒)
     */
    private Long avgResponseTime;

    /**
     * 最后使用时间 (ISO8601格式或时间戳)
     */
    private Long lastUsedDt;

    /**
     * 创建时间戳 (毫秒)
     */
    private Long createdDt;

    /**
     * 更新时间戳 (毫秒)
     */
    private Long updatedDt;
}
