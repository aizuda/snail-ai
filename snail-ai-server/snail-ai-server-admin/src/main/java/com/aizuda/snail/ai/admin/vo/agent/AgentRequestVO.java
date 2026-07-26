package com.aizuda.snail.ai.admin.vo.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * 智能体新增/更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AgentRequestVO {

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
     * 预设问题列表（第一个问题会同步到 greeting）
     */
    private List<String> presetQuestions;

    /** 对话模型 ID */
    private Long chatModelId;

    /** 是否启用 MCP 工具 */
    private Boolean mcpEnabled;

    /** 是否启用技能 */
    private Boolean skillEnabled;

    /** 是否启用联网搜索 */
    private Boolean webSearchEnabled;

    /** 是否启用知识库（RAG） */
    @JsonAlias("knowledgeSpaceEnabled")
    private Boolean ragEnabled;

    /** 是否启用记忆 */
    private Boolean memoryEnabled;

    /** 记忆检索配置 ID */
    private Long memoryConfigId;

    /** 智能体绑定 RAG ID 列表（逗号分隔，最多5个） */
    private String ragIds;

    /** RAG 调用方式: 1=智能调用 2=强制调用 */
    private Integer ragCallMode;

    /** 短期记忆滑动窗口保留条数，默认 20 */
    private Integer shortTermMemorySize;

    /** 是否企业精选推荐 */
    private Boolean isFeatured;

    /** 绑定的 MCP 服务 ID 列表 */
    private List<Long> mcpServerIds;

    /** 绑定的技能 ID 列表 */
    private List<Long> skillIds;

    /** 关联应用ID（NULL=本地执行） */
    private String appId;
}
