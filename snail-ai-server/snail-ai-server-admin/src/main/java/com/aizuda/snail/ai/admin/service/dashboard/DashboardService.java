package com.aizuda.snail.ai.admin.service.dashboard;

import com.aizuda.snail.ai.admin.service.model.AiModelUsageService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.dashboard.DashboardOverviewVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatVO;
import com.aizuda.snail.ai.admin.vo.model.AiModelUsageStatsQueryVO;
import com.aizuda.snail.ai.features.rag.enums.RagDocumentStatus;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentSkillMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentUsageStatMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页运营概览聚合服务：服务端一次性聚合 KPI/趋势/排行/最近对话/知识库/技能/资源，
 * 取代前端逐个接口聚合（消除 agent analytics N+1 与重复请求）。
 * <p>
 * 性能约定：所有随业务增长而膨胀的指标（用量 KPI、按日趋势、智能体排行、资源分配会话数）
 * 一律在数据库侧 {@code GROUP BY} 聚合，返回行数有界（单行 / 至多区间天数 / 至多活跃智能体数），
 * 不再把 {@code sai_agent_usage_stat} 全量明细拉进内存再聚合，从根本上规避大数据量下的
 * 内存溢出与响应卡顿。目录型小表（智能体、知识库、技能）数量有界，按需整表读取。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 时间范围入参：近 30 天。 */
    private static final String RANGE_30D = "30d";
    /** 近 30 天对应的天数。 */
    private static final int DAYS_30 = 30;
    /** 默认时间范围（近 7 天）对应的天数。 */
    private static final int DAYS_7 = 7;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int RANKING_LIMIT = 8;
    private static final int RECENT_LIMIT = 8;
    private static final int TOP_KB_LIMIT = 3;
    private static final int TOP_SKILL_LIMIT = 5;
    private static final int RESOURCE_LIMIT = 10;
    /** 模型数量级很小，读时聚合结果一次取足即可。 */
    private static final int MODEL_STATS_PAGE_SIZE = 1000;

    /** 资源分配加权分：知识库权重最高，其次技能，再次会话。集中为常量避免魔法值散落。 */
    private static final int WEIGHT_KB = 5;
    private static final int WEIGHT_SKILL = 3;
    private static final int WEIGHT_CONVERSATION = 2;

    private final AgentMapper agentMapper;
    private final AgentUsageStatMapper agentUsageStatMapper;
    private final AgentConversationMapper agentConversationMapper;
    private final AgentConversationRecordMapper agentConversationRecordMapper;
    private final AgentSkillMapper agentSkillMapper;
    private final RagMapper ragMapper;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;
    private final SkillMapper skillMapper;
    private final AiModelUsageService aiModelUsageService;

    /**
     * 聚合首页所需的全部数据。
     *
     * @param range 时间范围：7d / 30d，默认 7d
     * @return 首页概览聚合结果
     */
    public DashboardOverviewVO getOverview(String range) {
        long startedAt = System.currentTimeMillis();
        int days = RANGE_30D.equals(range) ? DAYS_30 : DAYS_7;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        log.info("首页概览聚合开始, range={}, start={}, end={}", range, start, end);

        // 智能体为管理员维护的目录表（数量有界），整表加载用于排行/资源的名称、头像回显
        List<AgentPO> agents = agentMapper.selectList(null);
        Map<Long, AgentPO> agentById = agents.stream()
                .collect(Collectors.toMap(AgentPO::getId, a -> a, (a, b) -> a));

        // 用量统计一律库侧聚合：总量单行、趋势至多区间天数行、排行至多活跃智能体行
        Map<String, Object> overviewTotals = agentUsageStatMapper.sumOverview(start, end);
        List<Map<String, Object>> dailyTrends = agentUsageStatMapper.aggregateDailyTrends(start, end);
        List<Map<String, Object>> dailyTokenTrends = agentConversationRecordMapper.aggregateDailyTokenTrends(start, end);
        List<Map<String, Object>> agentTotals = agentUsageStatMapper.aggregateAgentTotals(start, end);

        List<DashboardOverviewVO.ModelTokenStat> modelRanking = buildModelRanking();
        // Token 总量与按日趋势同源（对话记录读时聚合），避免排行过滤条件导致 KPI 与曲线口径不一致
        long totalTokens = dailyTokenTrends.stream()
                .mapToLong(row -> numOf(row, "promptTokens") + numOf(row, "completionTokens"))
                .sum();

        DashboardOverviewVO result = DashboardOverviewVO.builder()
                .overview(buildOverview(overviewTotals, totalTokens))
                .trends(buildTrends(dailyTrends, dailyTokenTrends, start, end))
                .modelRanking(modelRanking.stream().limit(RANKING_LIMIT).collect(Collectors.toList()))
                .agentRanking(buildAgentRanking(agentTotals, agentById))
                .recentConversations(buildRecentConversations(agentById))
                .knowledge(buildKnowledge())
                .skills(buildSkills())
                .resourceAllocation(buildResourceAllocation(agents, agentTotals))
                .build();

        log.info("首页概览聚合完成, range={}, 智能体数={}, 模型数={}, 耗时={}ms",
                range, agents.size(), modelRanking.size(), System.currentTimeMillis() - startedAt);
        return result;
    }

    /**
     * 构建顶部 KPI 概览。
     *
     * @param totals      库侧汇总的总量（会话数/消息数/活跃用户数）
     * @param totalTokens 模型排行汇总出的总 token 消耗
     * @return KPI 概览
     */
    private DashboardOverviewVO.Overview buildOverview(Map<String, Object> totals, long totalTokens) {
        // avgResponseTime 暂无真实耗时数据源（对话记录未落库时延），保留字段置 0，首页 KPI 改用真实消息总量
        return DashboardOverviewVO.Overview.builder()
                .totalConversations(numOf(totals, "totalConversations"))
                .totalMessages(numOf(totals, "totalMessages"))
                .activeUsers(numOf(totals, "activeUsers"))
                .totalTokensUsed(totalTokens)
                .avgResponseTime(0)
                .build();
    }

    /**
     * 构建按日趋势序列。数据库已按日聚合，这里仅按区间逐日补齐缺失日期为 0，保证曲线连续。
     *
     * @param dailyRows      库侧按日聚合的调用行（至多为区间天数）
     * @param dailyTokenRows 库侧按日聚合的 Token 行（至多为区间天数）
     * @param start          区间起始日（含）
     * @param end            区间结束日（含）
     * @return 会话/消息/工具调用/活跃用户/活跃智能体/Prompt·Completion Token 的按日序列
     */
    private DashboardOverviewVO.Trends buildTrends(List<Map<String, Object>> dailyRows,
                                                   List<Map<String, Object>> dailyTokenRows,
                                                   LocalDate start,
                                                   LocalDate end) {
        Map<LocalDate, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> row : dailyRows) {
            LocalDate date = dateOf(row, "statDate");
            if (date != null) {
                byDate.put(date, row);
            }
        }
        Map<LocalDate, Map<String, Object>> tokenByDate = new HashMap<>();
        for (Map<String, Object> row : dailyTokenRows) {
            LocalDate date = dateOf(row, "statDate");
            if (date != null) {
                tokenByDate.put(date, row);
            }
        }

        List<String> dateLabels = new ArrayList<>();
        List<Long> conversationTrend = new ArrayList<>();
        List<Long> messageTrend = new ArrayList<>();
        List<Long> toolCallTrend = new ArrayList<>();
        List<Long> activeUsersTrend = new ArrayList<>();
        List<Long> activeAgentsTrend = new ArrayList<>();
        List<Long> promptTokenTrend = new ArrayList<>();
        List<Long> completionTokenTrend = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> row = byDate.get(date);
            Map<String, Object> tokenRow = tokenByDate.get(date);
            dateLabels.add(date.format(ISO_DATE));
            conversationTrend.add(numOf(row, "conversationCount"));
            messageTrend.add(numOf(row, "messageCount"));
            // 工具调用暂无数据源，占位 0，保持前端曲线结构一致
            toolCallTrend.add(0L);
            activeUsersTrend.add(numOf(row, "activeUsers"));
            activeAgentsTrend.add(numOf(row, "activeAgents"));
            promptTokenTrend.add(numOf(tokenRow, "promptTokens"));
            completionTokenTrend.add(numOf(tokenRow, "completionTokens"));
        }

        return DashboardOverviewVO.Trends.builder()
                .dateLabels(dateLabels)
                .conversationTrend(conversationTrend)
                .messageTrend(messageTrend)
                .toolCallTrend(toolCallTrend)
                .activeUsersTrend(activeUsersTrend)
                .activeAgentsTrend(activeAgentsTrend)
                .promptTokenTrend(promptTokenTrend)
                .completionTokenTrend(completionTokenTrend)
                .build();
    }

    /**
     * 构建模型 Token 消耗排行。
     * <p>
     * 复用 {@link AiModelUsageService#getGlobalModelStats} 的读时聚合（按模型 {@code GROUP BY}，
     * 结果集为模型数量级），一次取足后按 token 截断为排行。
     *
     * @return 模型 token 用量排行
     */
    private List<DashboardOverviewVO.ModelTokenStat> buildModelRanking() {
        AiModelUsageStatsQueryVO query = new AiModelUsageStatsQueryVO();
        query.setPage(1);
        query.setSize(MODEL_STATS_PAGE_SIZE);
        PageResult<List<AiModelUsageStatVO>> page = aiModelUsageService.getGlobalModelStats(query);
        List<AiModelUsageStatVO> records = page.getData() != null ? page.getData() : List.of();
        return records.stream()
                .map(vo -> DashboardOverviewVO.ModelTokenStat.builder()
                        .modelId(vo.getModelId())
                        .modelName(vo.getModelName())
                        .providerName(vo.getProviderName())
                        .totalTokensUsed(nz(vo.getTotalTokensUsed()))
                        .totalCalls(nz(vo.getTotalCalls()))
                        .successRate(vo.getSuccessRate())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建智能体排行（按会话数倒序取 Top N）。
     *
     * @param agentTotals 库侧按智能体聚合的会话/消息数
     * @param agentById   智能体基础信息（用于回显名称、头像）
     * @return 智能体排行
     */
    private List<DashboardOverviewVO.AgentRank> buildAgentRanking(List<Map<String, Object>> agentTotals,
                                                                  Map<Long, AgentPO> agentById) {
        return agentTotals.stream()
                .map(row -> {
                    long agentId = numOf(row, "agentId");
                    AgentPO agent = agentById.get(agentId);
                    return DashboardOverviewVO.AgentRank.builder()
                            .agentId(agentId)
                            .name(agent != null ? agent.getName() : null)
                            .avatar(agent != null ? agent.getAvatar() : null)
                            .conversationCount(numOf(row, "conversationCount"))
                            .messageCount(numOf(row, "messageCount"))
                            .avgResponseTime(0)
                            .build();
                })
                .sorted(Comparator.comparingLong(DashboardOverviewVO.AgentRank::getConversationCount).reversed())
                .limit(RANKING_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * 构建最近对话列表（按更新时间倒序取 Top N，库侧分页截断）。
     *
     * @param agentById 智能体基础信息（用于回显名称、头像）
     * @return 最近对话
     */
    private List<DashboardOverviewVO.RecentConversation> buildRecentConversations(Map<Long, AgentPO> agentById) {
        // 交由分页插件按配置的数据库方言生成分页 SQL（跨 MySQL/PG/达梦可移植），避免手写 LIMIT；结果有界，关闭 count 查询
        Page<AgentConversationPO> page = Page.of(1, RECENT_LIMIT);
        page.setSearchCount(false);
        List<AgentConversationPO> conversations = agentConversationMapper.selectPage(page,
                new LambdaQueryWrapper<AgentConversationPO>()
                        .orderByDesc(AgentConversationPO::getUpdateDt)).getRecords();
        return conversations.stream().map(c -> {
            AgentPO agent = agentById.get(c.getAgentId());
            LocalDateTime last = c.getUpdateDt() != null ? c.getUpdateDt() : c.getCreateDt();
            return DashboardOverviewVO.RecentConversation.builder()
                    .agentId(c.getAgentId())
                    .agentName(agent != null ? agent.getName() : null)
                    .agentAvatar(agent != null ? agent.getAvatar() : null)
                    .conversationId(c.getConversationId())
                    .title(c.getTitle() != null && !c.getTitle().isBlank() ? c.getTitle() : c.getConversationId())
                    .lastMessageDt(last != null ? last.toString() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 构建知识库概览（总量 + 解析失败数 + 文档数 Top N）。
     * <p>
     * 计数走库侧 {@code COUNT}/{@code GROUP BY}；知识库为目录型小表，整表读取后取 Top N。
     *
     * @return 知识库概览
     */
    private DashboardOverviewVO.Knowledge buildKnowledge() {
        long totalKb = ragMapper.selectCount(null);
        long totalDocs = ragDocumentMapper.selectCount(null);
        long totalChunks = ragChunkMapper.selectCount(null);

        Map<Long, Long> docCountByRag = new HashMap<>();
        for (Map<String, Object> row : ragDocumentMapper.countDocumentByRag()) {
            docCountByRag.put(numOf(row, "ragId"), numOf(row, "cnt"));
        }

        long failedDocuments = ragDocumentMapper.countByStatus(RagDocumentStatus.FAILED.getStatus());

        List<DashboardOverviewVO.TopKnowledgeBase> top = ragMapper.selectList(null).stream()
                .map(rag -> DashboardOverviewVO.TopKnowledgeBase.builder()
                        .id(rag.getId())
                        .name(rag.getName())
                        .icon(rag.getIcon())
                        .documentCount(docCountByRag.getOrDefault(rag.getId(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(DashboardOverviewVO.TopKnowledgeBase::getDocumentCount).reversed())
                .limit(TOP_KB_LIMIT)
                .collect(Collectors.toList());

        return DashboardOverviewVO.Knowledge.builder()
                .totalKnowledgeBases(totalKb)
                .totalDocuments(totalDocs)
                .totalChunks(totalChunks)
                .failedDocuments(failedDocuments)
                .topKnowledgeBases(top)
                .build();
    }

    /**
     * 构建技能概览（总量 + 被绑定智能体数 Top N）。
     * <p>
     * 绑定数走库侧 {@code GROUP BY}；技能为目录型小表，整表读取后取 Top N。
     *
     * @return 技能概览
     */
    private DashboardOverviewVO.Skills buildSkills() {
        long totalSkills = skillMapper.selectCount(null);

        Map<Long, Long> agentCountBySkill = new HashMap<>();
        for (Map<String, Object> row : agentSkillMapper.countAgentBySkill()) {
            agentCountBySkill.put(numOf(row, "skillId"), numOf(row, "cnt"));
        }

        List<DashboardOverviewVO.TopSkill> top = skillMapper.selectList(null).stream()
                .map(skill -> DashboardOverviewVO.TopSkill.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .fileSize(skill.getFileSize())
                        .agentCount(agentCountBySkill.getOrDefault(skill.getId(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(DashboardOverviewVO.TopSkill::getAgentCount).reversed())
                .limit(TOP_SKILL_LIMIT)
                .collect(Collectors.toList());

        return DashboardOverviewVO.Skills.builder()
                .totalSkills(totalSkills)
                .skills(top)
                .build();
    }

    /**
     * 构建资源分配（按知识库/技能/会话加权分倒序取 Top N）。
     *
     * @param agents      全部智能体（目录型小表）
     * @param agentTotals 库侧按智能体聚合的会话数
     * @return 资源分配排行
     */
    private List<DashboardOverviewVO.ResourceAgent> buildResourceAllocation(List<AgentPO> agents,
                                                                            List<Map<String, Object>> agentTotals) {
        Map<Long, Long> convByAgent = new HashMap<>();
        for (Map<String, Object> row : agentTotals) {
            convByAgent.put(numOf(row, "agentId"), numOf(row, "conversationCount"));
        }
        Map<Long, Long> skillCountByAgent = new HashMap<>();
        for (Map<String, Object> row : agentSkillMapper.countSkillByAgent()) {
            skillCountByAgent.put(numOf(row, "agentId"), numOf(row, "cnt"));
        }

        return agents.stream()
                .map(a -> {
                    long kb = countRagIds(a.getRagIds());
                    long sk = skillCountByAgent.getOrDefault(a.getId(), 0L);
                    long conv = convByAgent.getOrDefault(a.getId(), 0L);
                    return DashboardOverviewVO.ResourceAgent.builder()
                            .id(a.getId())
                            .name(a.getName())
                            .avatar(a.getAvatar())
                            .knowledgeBases(kb)
                            .skills(sk)
                            .conversations(conv)
                            .build();
                })
                .sorted(Comparator.comparingLong(
                        (DashboardOverviewVO.ResourceAgent r) ->
                                r.getConversations() * WEIGHT_CONVERSATION
                                        + r.getSkills() * WEIGHT_SKILL
                                        + r.getKnowledgeBases() * WEIGHT_KB).reversed())
                .limit(RESOURCE_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * 统计智能体绑定的知识库数量。知识库以逗号分隔的字符串存储在智能体上。
     *
     * @param ragIds 逗号分隔的知识库 ID 串，可能为 null/空
     * @return 知识库数量
     */
    private long countRagIds(String ragIds) {
        if (ragIds == null || ragIds.isBlank()) {
            return 0L;
        }
        return Arrays.stream(ragIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).count();
    }

    /** 空值转 0（Long）。 */
    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    /**
     * 从结果 Map 取值，兼容不同数据库对列别名大小写的处理（原样 / 全小写 / 全大写）。
     *
     * @param row 结果行，可能为 null
     * @param key 列别名
     * @return 命中的值或 null
     */
    private static Object valueOf(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        Object v = row.get(key);
        if (v == null) {
            v = row.get(key.toLowerCase());
        }
        if (v == null) {
            v = row.get(key.toUpperCase());
        }
        return v;
    }

    /**
     * 从结果 Map 取 long 值，缺失或非数值返回 0。
     *
     * @param row 结果行，可能为 null
     * @param key 列别名
     * @return long 值
     */
    private static long numOf(Map<String, Object> row, String key) {
        Object v = valueOf(row, key);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * 从结果 Map 取日期，兼容 {@link LocalDate}、{@link java.sql.Date}、{@link java.util.Date} 与字符串等 JDBC 返回类型。
     *
     * @param row 结果行
     * @param key 列别名
     * @return 解析出的日期或 null
     */
    private static LocalDate dateOf(Map<String, Object> row, String key) {
        Object v = valueOf(row, key);
        if (v instanceof LocalDate date) {
            return date;
        }
        if (v instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (v instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (v instanceof String s && s.length() >= 10) {
            return LocalDate.parse(s.substring(0, 10));
        }
        return null;
    }
}
