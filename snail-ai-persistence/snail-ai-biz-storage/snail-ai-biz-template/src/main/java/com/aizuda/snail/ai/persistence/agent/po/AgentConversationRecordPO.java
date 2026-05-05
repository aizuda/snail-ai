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
 * 对话记录持久化对象
 * 表: snail_ai_agent_conversation_record
 *
 * 记录用户与Agent间的每条对话消息
 * 支持多轮对话追踪、上下文恢复、记忆提取等
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_agent_conversation_record")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentConversationRecordPO {

    /**
     * 记录ID (主键)
     * 自增主键，全局唯一
     * 同时用于记忆检查点追踪
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 智能体ID (外键)
     * 关联到 snail_ai_agent.id
     * 标识该消息来自于哪个Agent
     */
    private Long agentId;

    /**
     * 会话ID (外键)
     * 关联到 snail_ai_agent_conversation.id
     * 同一会话内的消息共享相同的conversationId，用于多轮对话关联
     */
    private String conversationId;

    /**
     * 用户ID (外键)
     * 关联到 snail_ai_user.id
     * 发送消息的用户
     */
    private Long userId;

    /**
     * 消息角色
     * USER: 用户消息
     * ASSISTANT: 助手/Agent回复
     * SYSTEM: 系统消息
     */
    private String role;

    /**
     * 消息内容
     * 完整的消息文本内容
     * 可能包含markdown格式、代码块等
     */
    private String content;

    /**
     * 思考过程
     * 模型的思维链/推理过程（仅 assistant 角色）
     */
    private String thinking;

    /**
     * 消息状态
     * 0: 草稿/未发送
     * 1: 已发送
     * 2: 已处理
     * -1: 处理失败
     */
    private Integer status;

    /**
     * Token数量统计
     * 该条消息的Token消耗数量
     * 用于统计和费用计算
     */
    private Integer tokenCount;

    /**
     * 创建时间
     * 消息记录的创建时刻
     */
    private LocalDateTime createDt;
}
