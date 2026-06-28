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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentResponseVO {

    private Long id;

    private String name;

    private String description;

    private String avatar;

    private String instruction;

    private String greeting;

    /**
     * 预设问题列表
     */
    private List<String> presetQuestions;

    private Long chatModelId;

    private String chatModel;

    private Boolean mcpEnabled;

    private List<McpServerResponseVO> mcpServers;

    private Boolean skillEnabled;

    private List<SkillResponseVO> skills;

    private Boolean webSearchEnabled;

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

    private String creator;

    private Integer viewCount;

    private Boolean isFeatured;

    /**
     * Agent状态
     * @see com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum
     */
    private Integer status;

    /** 关联应用ID（NULL=本地执行） */
    private String appId;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;

    /** 用户是否已订阅此智能体（仅市场接口返回） */
    private Boolean subscribed;
}
