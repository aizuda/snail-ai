package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationMapper;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationPO;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OpenAPI 会话管理服务
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Service
@RequiredArgsConstructor
public class OpenApiConversationService {

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationRecordMapper conversationRecordMapper;
    private final OpenApiUserResolver openApiUserResolver;

    public PageResult<List<OpenApiConversationVO>> listConversations(OpenApiConversationQueryRequest request) {
        Long userId = resolveUserId(request.getOpenId());

        Page<AgentConversationPO> pageParam = new Page<>(request.getPage(), request.getSize());
        Page<AgentConversationPO> result = conversationMapper.selectPage(pageParam,
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationPO::getUserId, userId)
                        .orderByDesc(AgentConversationPO::getUpdateDt));
        List<OpenApiConversationVO> conversations = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return pageResult(result, conversations);
    }

    public OpenApiConversationVO createConversation(OpenApiCreateConversationRequest request) {
        Long userId = resolveUserId(request.getOpenId());

        AgentConversationPO po = AgentConversationPO.builder()
                .agentId(request.getAgentId())
                .userId(userId)
                .conversationId(UUID.randomUUID().toString())
                .title(request.getTitle())
                .createDt(LocalDateTime.now())
                .updateDt(LocalDateTime.now())
                .build();
        conversationMapper.insert(po);
        return toVO(po);
    }

    public void deleteConversation(OpenApiConversationIdentityRequest request) {
        Long userId = resolveUserId(request.getOpenId());

        conversationMapper.delete(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationPO::getConversationId, request.getConversationId())
                        .eq(AgentConversationPO::getUserId, userId));

        conversationRecordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationRecordPO::getConversationId, request.getConversationId())
                        .eq(AgentConversationRecordPO::getUserId, userId));
    }

    public List<OpenApiMessageVO> getMessages(OpenApiConversationIdentityRequest request) {
        Long userId = resolveUserId(request.getOpenId());

        List<AgentConversationRecordPO> records = conversationRecordMapper.selectList(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationRecordPO::getConversationId, request.getConversationId())
                        .eq(AgentConversationRecordPO::getUserId, userId)
                        .orderByAsc(AgentConversationRecordPO::getCreateDt));

        return records.stream().map(r -> OpenApiMessageVO.builder()
                .role(r.getRole())
                .content(r.getContent())
                .thinking(r.getThinking())
                .status(r.getStatus())
                .createDt(toDateTimeString(r.getCreateDt()))
                .build()).collect(Collectors.toList());
    }

    private OpenApiConversationVO toVO(AgentConversationPO po) {
        return OpenApiConversationVO.builder()
                .conversationId(po.getConversationId())
                .agentId(po.getAgentId())
                .title(po.getTitle())
                .createDt(toDateTimeString(po.getCreateDt()))
                .updateDt(toDateTimeString(po.getUpdateDt()))
                .build();
    }

    private PageResult<List<OpenApiConversationVO>> pageResult(Page<AgentConversationPO> page,
                                                               List<OpenApiConversationVO> conversations) {
        PageResult<List<OpenApiConversationVO>> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setData(conversations);
        return result;
    }

    private String toDateTimeString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }

    private Long resolveUserId(String openId) {
        String appId = OpenApiSessionUtils.current().getAppId();
        UserPO user = openApiUserResolver.resolvePlatformUser(appId, openId);
        return user.getId();
    }
}
