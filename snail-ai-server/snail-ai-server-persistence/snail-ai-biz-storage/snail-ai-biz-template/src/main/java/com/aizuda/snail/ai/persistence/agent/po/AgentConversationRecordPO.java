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
 * 表: sai_agent_conversation_record
 *
 * 记录用户与Agent间的每条对话消息
 * 支持多轮对话追踪、上下文恢复、记忆提取等
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_agent_conversation_record")
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
     * 关联到 sai_agent.id
     * 标识该消息来自于哪个Agent
     */
    private Long agentId;

    /**
     * 会话ID (外键)
     * 关联到 sai_agent_conversation.id
     * 同一会话内的消息共享相同的conversationId，用于多轮对话关联
     */
    private String conversationId;

    /**
     * 用户ID (外键)
     * 关联到 sai_user.id
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
     * 输入Token数（prompt）
     * 发送给模型的提示词所消耗的Token
     */
    private Integer inputTokens;

    /**
     * 输出Token数（completion）
     * 模型生成回复所消耗的Token
     */
    private Integer outputTokens;

    /**
     * 缓存命中Token数
     * 命中Prompt Cache的Token数量，可降低调用成本
     */
    private Integer cacheTokens;

    /**
     * 创建时间
     * 消息记录的创建时刻
     */
    private LocalDateTime createDt;
}
