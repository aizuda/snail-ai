package com.aizuda.snail.ai.admin.service.agent;

import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.agent.AgentAnalyticsQueryVO;
import com.aizuda.snail.ai.admin.vo.agent.AgentAnalyticsVO;
import com.aizuda.snail.ai.admin.vo.agent.AgentUsageDetailQueryVO;
import com.aizuda.snail.ai.admin.vo.agent.AgentUsageDetailVO;
import com.aizuda.snail.ai.persistence.admin.mapper.UserMapper;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentUsageStatMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentUsageStatPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentAnalyticsService {

    private final AgentUsageStatMapper usageStatMapper;
    private final UserMapper userMapper;
    /**
     * 获取分析概览
     */
    public AgentAnalyticsVO getAnalytics(Long agentId, AgentAnalyticsQueryVO queryVO) {
        String range = queryVO.getRange();
        String start = queryVO.getStart();
        String end = queryVO.getEnd();
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        boolean hasCustomDateRange = start != null && !start.isBlank() && end != null && !end.isBlank();
        if (hasCustomDateRange) {
            startDate = LocalDate.parse(start);
            endDate = LocalDate.parse(end);
            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }
        } else {
            int days = switch (range) {
                case "7d" -> 7;
                case "30d" -> 30;
                default -> 1;
            };
            startDate = endDate.minusDays(days - 1);
        }

        LambdaQueryWrapper<AgentUsageStatPO> wrapper = new LambdaQueryWrapper<AgentUsageStatPO>()
                .eq(AgentUsageStatPO::getAgentId, agentId)
                .ge(AgentUsageStatPO::getStatDate, startDate)
                .le(AgentUsageStatPO::getStatDate, endDate);

        List<AgentUsageStatPO> stats = usageStatMapper.selectList(wrapper);

        // 聚合统计
        Set<Long> uniqueUsers = new HashSet<>();
        int totalMessages = 0;
        int totalConversations = 0;

        for (AgentUsageStatPO stat : stats) {
            uniqueUsers.add(stat.getUserId());
            totalMessages += stat.getMessageCount() != null ? stat.getMessageCount() : 0;
            totalConversations += stat.getConversationCount() != null ? stat.getConversationCount() : 0;
        }

        Map<LocalDate, Integer> usersByDate = new HashMap<>();
        Map<LocalDate, Integer> convByDate = new HashMap<>();
        Map<LocalDate, Integer> messageByDate = new HashMap<>();

        for (AgentUsageStatPO stat : stats) {
            usersByDate.merge(stat.getStatDate(), 1, Integer::sum);
            convByDate.merge(stat.getStatDate(), stat.getConversationCount() != null ? stat.getConversationCount() : 0, Integer::sum);
            messageByDate.merge(stat.getStatDate(), stat.getMessageCount() != null ? stat.getMessageCount() : 0, Integer::sum);
        }

        List<String> dateLabels = new ArrayList<>();
        List<Integer> activeUsersTrend = new ArrayList<>();
        List<Integer> conversationTrend = new ArrayList<>();
        List<Integer> messageTrend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dateLabels.add(current.format(fmt));
            activeUsersTrend.add(usersByDate.getOrDefault(current, 0));
            conversationTrend.add(convByDate.getOrDefault(current, 0));
            messageTrend.add(messageByDate.getOrDefault(current, 0));
            current = current.plusDays(1);
        }

        int totalToolCalls = 0;
        double avgResponseTime = 0;

        return AgentAnalyticsVO.builder()
                .activeUsers(uniqueUsers.size())
                .activeUsersTrend(activeUsersTrend)
                .conversationCount(totalConversations)
                .conversationCountTrend(conversationTrend)
                .totalMessages(totalMessages)
                .dateLabels(dateLabels)
                .messageTrend(messageTrend)
                .totalToolCalls(totalToolCalls)
                .avgResponseTime(avgResponseTime)
                .dateRange(AgentAnalyticsVO.DateRange.builder()
                        .start(startDate.format(fmt))
                        .end(endDate.format(fmt))
                        .build())
                .build();
    }

    /**
     * 获取使用明细
     */
    public PageResult<List<AgentUsageDetailVO>> getUsageDetail(Long agentId, AgentUsageDetailQueryVO queryVO) {
        LambdaQueryWrapper<AgentUsageStatPO> wrapper = new LambdaQueryWrapper<AgentUsageStatPO>()
                .eq(AgentUsageStatPO::getAgentId, agentId);
        String start = queryVO.getStart();
        String end = queryVO.getEnd();
        int page = queryVO.getPage();
        int size = queryVO.getSize();

        if (start != null) {
            wrapper.ge(AgentUsageStatPO::getStatDate, LocalDate.parse(start));
        }
        if (end != null) {
            wrapper.le(AgentUsageStatPO::getStatDate, LocalDate.parse(end));
        }

        // 先查全量数据按用户分组
        List<AgentUsageStatPO> allStats = usageStatMapper.selectList(wrapper);

        Map<Long, AgentUsageDetailVO> userMap = new LinkedHashMap<>();
        for (AgentUsageStatPO stat : allStats) {
            userMap.merge(stat.getUserId(),
                    AgentUsageDetailVO.builder()
                            .userId(stat.getUserId())
                            .messageCount(stat.getMessageCount() != null ? stat.getMessageCount() : 0)
                            .build(),
                    (existing, newVal) -> {
                        existing.setMessageCount(existing.getMessageCount() + newVal.getMessageCount());
                        return existing;
                    });
        }

        // 批量查询用户名
        if (!userMap.isEmpty()) {
            List<UserPO> users = userMapper.selectByIds(userMap.keySet());
            Map<Long, String> userNameMap = new HashMap<>();
            for (UserPO user : users) {
                userNameMap.put(user.getId(), user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
            for (AgentUsageDetailVO detail : userMap.values()) {
                detail.setUserName(userNameMap.get(detail.getUserId()));
            }
        }

        List<AgentUsageDetailVO> allDetails = new ArrayList<>(userMap.values());

        // 手动分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, allDetails.size());
        List<AgentUsageDetailVO> pageData = fromIndex < allDetails.size()
                ? allDetails.subList(fromIndex, toIndex)
                : List.of();

        PageDTO<AgentUsageStatPO> pageDTO = new PageDTO<>(page, size);
        pageDTO.setTotal(allDetails.size());

        return new PageResult<>(pageDTO, pageData);
    }
}
