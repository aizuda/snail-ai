package com.aizuda.snail.ai.admin.vo.agent;

import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillResponseVO;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体详情响应（视图）对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentResponseVO {

    /** 智能体 ID */
    private Long id;

    /** 智能体名称 */
    private String name;

    /** 智能体描述 */
    private String description;

    /** 头像地址 */
    private String avatar;

    /** 系统指令（提示词） */
    private String instruction;

    /** 开场问候语 */
    private String greeting;

    /**
     * 预设问题列表
     */
    private List<String> presetQuestions;

    /** 对话模型 ID */
    private Long chatModelId;

    /** 对话模型名称 */
    private String chatModel;

    /** 是否启用 MCP 工具 */
    private Boolean mcpEnabled;

    /** 绑定的 MCP 服务列表 */
    private List<McpServerResponseVO> mcpServers;

    /** 是否启用技能 */
    private Boolean skillEnabled;

    /** 绑定的技能列表 */
    private List<SkillResponseVO> skills;

    /** 是否启用联网搜索 */
    private Boolean webSearchEnabled;

    /** 是否启用知识库（RAG） */
    @JsonAlias("knowledgeSpaceEnabled")
    private Boolean ragEnabled;

    /** 是否启用短期记忆上下文 */
    private Boolean memoryEnabled;

    /** 智能体绑定 RAG ID 列表（逗号分隔） */
    private String ragIds;

    /** RAG 调用方式: 1=智能调用 2=强制调用 */
    private Integer ragCallMode;

    /** 短期记忆滑动窗口保留条数 */
    private Integer shortTermMemorySize;

    /** 创建者 */
    private String creator;

    /** 查看次数 */
    private Integer viewCount;

    /** 是否企业精选推荐 */
    private Boolean isFeatured;

    /**
     * Agent状态
     * @see com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum
     */
    private Integer status;

    /** 关联应用ID（NULL=本地执行） */
    private String appId;

    /** 关联应用当前是否有可用客户端实例（市场接口用于订阅可用性提示） */
    private Boolean clientAvailable;

    /** 关联应用当前在线客户端实例数（市场接口返回） */
    private Integer onlineClientCount;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;

    /** 用户是否已订阅此智能体（仅市场接口返回） */
    private Boolean subscribed;
}
