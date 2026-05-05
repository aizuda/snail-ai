package com.aizuda.snail.ai.persistence.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话会话持久化对象
 * 表: snail_ai_agent_conversation
 *
 * 表示Agent与用户间的一个完整对话会话
 * 一个会话包含多条对话记录(AgentConversationRecordPO)
 * 支持会话管理、标题、创建时间等
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_agent_conversation")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentConversationPO {

    /**
     * 会话ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 智能体ID (外键)
     * 关联到 snail_ai_agent.id
     * 该会话所属的Agent
     */
    private Long agentId;

    /**
     * 用户ID (外键)
     * 关联到 snail_ai_user.id
     * 该会话的用户
     */
    private Long userId;

    /**
     * 会话唯一标识符
     * 业务层使用的会话ID(UUID格式)
     * 与自增id不同，用于外部引用
     */
    private String conversationId;

    /**
     * 会话标题
     * 用户自定义或系统生成的会话名称
     * 例如: "如何学习Java", "产品需求讨论"
     */
    private String title;

    /**
     * 创建时间
     * 会话首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 会话最后一次更新的时刻（通常是最后一条消息的时间）
     */
    private LocalDateTime updateDt;
}
