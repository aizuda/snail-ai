package com.aizuda.snail.ai.admin.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页运营概览聚合 VO。
 * <p>
 * 服务端一次性聚合 KPI、趋势、模型/智能体排行、最近对话、知识库/技能/资源概览，
 * 供首页单请求渲染，替代此前前端逐个聚合（N+1）的方式。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {

    private Overview overview;
    private Trends trends;
    private List<ModelTokenStat> modelRanking;
    private List<AgentRank> agentRanking;
    private List<RecentConversation> recentConversations;
    private Knowledge knowledge;
    private Skills skills;
    private List<ResourceAgent> resourceAllocation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private long totalConversations;
        private long totalMessages;
        private long activeUsers;
        private long totalTokensUsed;
        private long avgResponseTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trends {
        private List<String> dateLabels;
        private List<Long> conversationTrend;
        private List<Long> messageTrend;
        private List<Long> toolCallTrend;
        private List<Long> activeUsersTrend;
        private List<Long> activeAgentsTrend;
        /** 按日 Prompt Token（对话记录读时聚合） */
        private List<Long> promptTokenTrend;
        /** 按日 Completion Token（对话记录读时聚合） */
        private List<Long> completionTokenTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelTokenStat {
        private Long modelId;
        private String modelName;
        private String providerName;
        private long totalTokensUsed;
        private long totalCalls;
        private Double successRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentRank {
        private Long agentId;
        private String name;
        private String avatar;
        private long conversationCount;
        private long messageCount;
        private long avgResponseTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentConversation {
        private Long agentId;
        private String agentName;
        private String agentAvatar;
        private String conversationId;
        private String title;
        private String lastMessageDt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Knowledge {
        private long totalKnowledgeBases;
        private long totalDocuments;
        private long totalChunks;
        private long failedDocuments;
        private List<TopKnowledgeBase> topKnowledgeBases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopKnowledgeBase {
        private Long id;
        private String name;
        private long documentCount;
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skills {
        private long totalSkills;
        private List<TopSkill> skills;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSkill {
        private Long id;
        private String name;
        private String description;
        private long agentCount;
        private Long fileSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAgent {
        private Long id;
        private String name;
        private String avatar;
        private long knowledgeBases;
        private long skills;
        private long conversations;
    }
}
