package com.aizuda.snail.ai.persistence.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体（Agent）信息持久化对象
 * 表: sai_agent
 * <p>
 * 代表一个AI助手/Agent，包含其配置、能力、记忆、知识库等信息
 * 支持多种能力组合：MCP、技能、网络搜索、知识库、记忆等
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_agent")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentPO {

    /**
     * 智能体ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 智能体名称
     * 用户友好的显示名称
     */
    private String name;

    /**
     * 智能体描述
     * 简要描述该Agent的功能和用途
     */
    private String description;

    /**
     * 智能体头像URL
     * Agent在UI中显示的头像/图标
     */
    private String avatar;

    /**
     * 系统指令(System Prompt)
     * 发送给大模型的系统级提示词，定义Agent的角色和行为准则
     */
    private String instruction;

    /**
     * 问候语
     * 用户首次与Agent对话时显示的欢迎信息，不会作为推荐问题展示
     */
    private String greeting;

    /**
     * 预设问题列表（JSON数组字符串）
     * 用于对话页面的推荐问题按钮，与 greeting 独立
     */
    private String presetQuestions;

    /**
     * 聊天模型ID (外键)
     * 关联到 sai_model_config.id
     * 该Agent使用的主要LLM模型
     */
    private Long chatModelId;

    /**
     * 是否启用MCP (Model Context Protocol)
     * true: 启用MCP服务器集成
     * false: 禁用MCP
     */
    private Boolean mcpEnabled;

    /**
     * 是否启用技能(Skill)
     * true: Agent可以调用预定义的技能
     * false: 禁用技能调用
     */
    private Boolean skillEnabled;

    /**
     * 是否启用网络搜索
     * true: Agent可以进行实时网络搜索
     * false: 禁用网络搜索
     */
    private Boolean webSearchEnabled;

    /**
     * 是否启用 RAG
     * true: Agent 可以检索 RAG 中的相关文档
     * false: 禁用 RAG 检索
     */
    @TableField("rag_enabled")
    private Boolean ragEnabled;

    /**
     * 绑定的 RAG ID 列表（逗号分隔，最多5个）
     * 例如: "1,2,3"
     * 关联到 sai_rag.id
     */
    @TableField("rag_ids")
    private String ragIds;

    /**
     * RAG 调用方式
     * @see com.aizuda.snail.ai.common.enums.agent.RagCallModeEnum
     */
    @TableField("rag_call_mode")
    private Integer ragCallMode;

    /**
     * 是否启用多轮对话长期记忆
     * true: 启用长期记忆（跨多轮对话保留重要信息）
     * false: 禁用长期记忆
     */
    private Boolean memoryEnabled;

    /**
     * 短期记忆滑动窗口保留条数
     * 定义多轮对话中保留的上下文消息数量
     * 例如: 10表示保留最近10条消息作为上下文
     */
    private Integer shortTermMemorySize;

    /**
     * 创建者用户ID (外键, 可为null)
     * 关联到 sai_user.id
     * 该Agent的创建人，用于权限管理和溯源
     */
    private Long creatorId;

    /**
     * 是否为推荐Agent
     * true: 在列表中标记为特色/推荐
     * false: 普通Agent
     */
    private Boolean isFeatured;

    /**
     * 访问次数统计
     * 该Agent被查看或使用的累计次数，用于热度排序
     */
    private Integer viewCount;

    /**
     * Agent状态
     * @see com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum
     */
    private Integer status;

    /**
     * 配置JSON (JSONB格式)
     * 存储扩展配置参数
     * 例如: {"timeout": 30000, "retryCount": 3}
     */
    private String config;

    /**
     * 关联应用ID (外键, 可为null)
     * 关联到 sai_app.app_id
     * NULL: 本地执行
     * 具体值: 远程应用执行（分布式模式）
     */
    private String appId;

    /**
     * 创建时间
     * Agent首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * Agent最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
