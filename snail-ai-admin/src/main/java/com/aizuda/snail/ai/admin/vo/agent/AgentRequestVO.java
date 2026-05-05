package com.aizuda.snail.ai.admin.vo.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class AgentRequestVO {

    private String name;

    private String description;

    private String avatar;

    private String instruction;

    private String greeting;

    /**
     * 预设问题列表（第一个问题会同步到 greeting）
     */
    private List<String> presetQuestions;

    private Long chatModelId;

    private Boolean mcpEnabled;

    private Boolean skillEnabled;

    private Boolean webSearchEnabled;

    @JsonAlias("knowledgeSpaceEnabled")
    private Boolean ragEnabled;

    private Boolean memoryEnabled;

    /** 记忆检索配置 ID */
    private Long memoryConfigId;

    /** 智能体绑定 RAG ID */
    @JsonAlias("knowledgeId")
    private Long ragId;

    /** 短期记忆滑动窗口保留条数，默认 20 */
    private Integer shortTermMemorySize;

    /** 是否企业精选推荐 */
    private Boolean isFeatured;

    private List<Long> mcpServerIds;

    private List<Long> skillIds;

    /** 关联应用ID（NULL=本地执行） */
    private String appId;
}
