package com.aizuda.snail.ai.persistence.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agent使用统计持久化对象
 * 表: sai_agent_usage_stat
 *
 * 按Agent和用户维度统计使用情况
 * 按天聚合统计消息数、会话数等指标
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_agent_usage_stat")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentUsageStatPO {

    /**
     * 统计ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Agent ID (外键)
     * 关联到 sai_agent.id
     */
    private Long agentId;

    /**
     * 用户ID (外键)
     * 关联到 sai_user.id
     */
    private Long userId;

    /**
     * 消息数
     * 该天该Agent该用户的消息总数
     */
    private Integer messageCount;

    /**
     * 会话数
     * 该天该Agent该用户新创建的会话数
     */
    private Integer conversationCount;

    /**
     * 统计日期
     * 统计所针对的日期
     */
    private LocalDate statDate;

    /**
     * 创建时间
     * 统计记录创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 统计记录最后更新的时刻
     */
    private LocalDateTime updateDt;
}
