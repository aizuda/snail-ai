package com.aizuda.snail.ai.persistence.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型使用统计持久化对象
 * 表: snail_ai_model_usage_stat
 *
 * 按模型和用户维度统计使用情况
 * 定期从usage_log表聚合更新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("snail_ai_model_usage_stat")
public class AiModelUsageStatPO {

    /**
     * 统计记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模型ID (外键)
     * 关联到 snail_ai_model_config.id
     */
    private Long modelId;

    /**
     * 用户ID (外键)
     * 关联到 snail_ai_user.id
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
     * 总Token使用量
     */
    private Long totalTokensUsed;

    /**
     * 总费用 (可选)
     * 如果配置中有pricing信息，则计算该值
     */
    private BigDecimal totalCost;

    /**
     * 平均响应时间 (毫秒)
     */
    private Long avgResponseTime;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedDt;

    /**
     * 创建时间
     */
    private LocalDateTime createdDt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDt;
}
