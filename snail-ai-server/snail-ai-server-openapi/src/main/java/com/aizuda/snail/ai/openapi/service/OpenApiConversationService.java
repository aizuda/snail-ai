package com.aizuda.snail.ai.openapi.service;

import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationClearRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationUpdateTitleRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OpenAPI 会话管理服务
 *
 * @author opensnail
 * @since 2026-04-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiConversationService {

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationRecordMapper conversationRecordMapper;
    private final OpenApiUserResolver openApiUserResolver;

    /**
     * 分页查询指定智能体下当前用户的会话列表
     *
     * @param request 会话分页查询请求（智能体ID、openId、分页参数）
     * @return 会话分页结果
     */
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

    /**
     * 创建会话
     *
     * @param request 创建会话请求（智能体ID、openId、标题）
     * @return 新建的会话 VO
     */
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
        log.info("OpenAPI 创建会话 agentId={} conversationId={} userId={}", request.getAgentId(), po.getConversationId(), userId);
        return toVO(po);
    }

    /**
     * 删除会话及其消息记录
     *
     * @param request 会话标识请求（智能体ID、会话ID、openId）
     */
    @Transactional
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
        log.info("OpenAPI 删除会话 agentId={} conversationId={} userId={}", request.getAgentId(), request.getConversationId(), userId);
    }

    /**
     * 清空指定智能体下当前用户的全部会话与消息记录
     *
     * @param request 清空会话请求（智能体ID、openId）
     */
    @Transactional
    public void clearConversations(OpenApiConversationClearRequest request) {
        Long userId = resolveUserId(request.getOpenId());

        conversationRecordMapper.delete(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationRecordPO::getUserId, userId));

        conversationMapper.delete(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationPO::getUserId, userId));
        log.info("OpenAPI 清空会话 agentId={} userId={}", request.getAgentId(), userId);
    }

    /**
     * 更新会话标题
     *
     * @param request 更新标题请求（智能体ID、会话ID、openId、标题）
     */
    public void updateConversationTitle(OpenApiConversationUpdateTitleRequest request) {
        Long userId = resolveUserId(request.getOpenId());
        AgentConversationPO po = conversationMapper.selectOne(
                new LambdaQueryWrapper<AgentConversationPO>()
                        .eq(AgentConversationPO::getAgentId, request.getAgentId())
                        .eq(AgentConversationPO::getConversationId, request.getConversationId())
                        .eq(AgentConversationPO::getUserId, userId));
        if (po == null) {
            log.warn("OpenAPI 更新会话标题失败，会话不存在 agentId={} conversationId={}",
                    request.getAgentId(), request.getConversationId());
            return;
        }
        po.setTitle(request.getTitle());
        po.setUpdateDt(LocalDateTime.now());
        conversationMapper.updateById(po);
        log.info("OpenAPI 更新会话标题 agentId={} conversationId={} title={}",
                request.getAgentId(), request.getConversationId(), request.getTitle());
    }

    /**
     * 获取会话内的消息列表（按创建时间升序）
     *
     * @param request 会话标识请求（智能体ID、会话ID、openId）
     * @return 消息 VO 列表
     */
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
