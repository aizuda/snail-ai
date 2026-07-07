package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.service.agent.*;
import com.aizuda.snail.ai.admin.stream.AgentCreateStreamHttpAdapter;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.UpdateConversationTitleRequestVO;
import com.aizuda.snail.ai.admin.vo.agent.*;
import com.aizuda.snail.ai.admin.service.mcp.McpServerService;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.admin.vo.memory.ConversationSummaryVO;
import com.aizuda.snail.ai.feature.agent.stream.ChatStreamWriter;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能体管理接口：CRUD、AI 辅助创建、对话、MCP 绑定、用量分析与智能体市场订阅。
 * <p>
 * 权限约定：{@code @LoginRequired} 默认要求 ADMIN 角色；标注 {@code role = USER}
 * 的端点对任意登录用户开放（如对话、市场、订阅、会话记录）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentCreateService agentCreateService;
    private final AgentCreateStreamHttpAdapter agentCreateStreamHttpAdapter;
    private final McpServerService mcpServerService;
    private final UserAgentService userAgentService;
    private final AgentConversationRecordMapper conversationRecordMapper;
    private final AgentChatService agentChatService;
    private final AgentAnalyticsService agentAnalyticsService;

    // ==================== CRUD ====================

    /** 分页查询智能体（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<AgentResponseVO>> page(AgentQueryVO query) {
        return agentService.page(query);
    }

    /** 按 ID 查询智能体详情（登录用户）。 */
    @GetMapping("/{id}")
    @LoginRequired(role = RoleEnum.USER)
    public Result<AgentResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(agentService.getById(id));
    }

    /** 更新智能体配置（管理员）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<AgentResponseVO> update(@PathVariable("id") Long id,
                                           @RequestBody AgentRequestVO request) {
        return Result.ok(agentService.update(id, request));
    }

    /** 删除智能体（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentService.delete(id);
        return Result.ok(null);
    }

    // ==================== Chat Models ====================

    /** 获取可用的对话模型列表（管理员）。 */
    @GetMapping("/chat-models")
    @LoginRequired
    public Result<List<Map<String, Object>>> chatModels() {
        return Result.ok(agentService.getChatModels());
    }

    // ==================== AI Create ====================

    /** 根据自然语言描述流式生成智能体配置（管理员，SSE 流）。 */
    @PostMapping(value = "/create/stream", produces = AgentCreateStreamHttpAdapter.MEDIA_TYPE)
    @LoginRequired
    public Flux<String> createStream(@RequestBody @Validated AgentCreateRequestVO request) {
        return agentCreateStreamHttpAdapter.encode(agentCreateService.createByDescriptionStream(request.getDescription()));
    }

    /** 创建智能体（管理员）。 */
    @PostMapping
    @LoginRequired
    public Result<AgentResponseVO> create(@RequestBody @Validated AgentRequestVO request) {
        return Result.ok(agentService.create(request));
    }

    /** 与指定智能体进行流式对话（登录用户，SSE 流）。 */
    @PostMapping(value = "/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @LoginRequired(role = RoleEnum.USER)
    public Flux<String> chat(@PathVariable("id") Long id,
                             @RequestBody @Validated AgentChatRequestVO request) {
        return Flux.<String>create(sink -> {
            agentChatService.chat(AgentChatCommand.builder()
                    .agentId(id)
                    .conversationId(request.getConversationId())
                    .content(request.getContent())
                    .attachments(request.getAttachments())
                    .disabledMcpServerIds(request.getDisabledMcpServerIds())
                    .disabledSkillIds(request.getDisabledSkillIds())
                    .streamWriter(new FluxBridgeChatStreamWriter(sink))
                    .build());
            sink.onCancel(() -> {});
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private static class FluxBridgeChatStreamWriter implements ChatStreamWriter {
        private final FluxSink<String> sink;

        FluxBridgeChatStreamWriter(FluxSink<String> sink) {
            this.sink = sink;
        }

        @Override
        public void send(String data) {
            if (sink.isCancelled()) {
                return;
            }
            sink.next(data);
        }

        @Override
        public void complete() {
            if (sink.isCancelled()) {
                return;
            }
            sink.complete();
        }

        @Override
        public void completeWithError(Throwable ex) {
            if (sink.isCancelled()) {
                return;
            }
            sink.complete();
        }
    }

    // ==================== MCP Servers ====================

    /** 查询智能体已绑定的 MCP 服务列表（管理员）。 */
    @GetMapping("/{id}/mcp-servers")
    @LoginRequired
    public Result<List<McpServerResponseVO>> getMcpServers(@PathVariable("id") Long id) {
        return Result.ok(mcpServerService.getByAgentId(id));
    }

    /** 更新智能体绑定的 MCP 服务（管理员）。 */
    @PutMapping("/{id}/mcp-servers")
    @LoginRequired
    public Result<Void> updateMcpServers(@PathVariable("id") Long id,
                                          @RequestBody AgentMcpServersUpdateRequestVO requestVO) {
        mcpServerService.updateAgentMcpServers(id, requestVO.getMcpServerIds());
        return Result.ok(null);
    }

    // ==================== Analytics ====================

    /** 查询智能体用量分析汇总（管理员）。 */
    @GetMapping("/{id}/analytics")
    @LoginRequired
    public Result<AgentAnalyticsVO> analytics(@PathVariable("id") Long id, AgentAnalyticsQueryVO queryVO) {
        return Result.ok(agentAnalyticsService.getAnalytics(id, queryVO));
    }

    /** 删除指定会话（登录用户，仅限本人会话）。 */
    @DeleteMapping("/{id}/conversation/{conversationId}")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> deleteConversation(
            @PathVariable("id") Long agentId,
            @PathVariable("conversationId") String conversationId) {
        agentService.deleteConversation(agentId, conversationId);
        return Result.ok(null);
    }

    /** 分页查询当前用户在该智能体下的会话列表（登录用户）。 */
    @GetMapping("/{id}/conversations")
    @LoginRequired(role = RoleEnum.USER)
    public PageResult<List<ConversationSummaryVO>> conversations(@PathVariable("id") Long id,
            AgentConversationQueryVO queryVO) {
        return agentService.listConversations(id, queryVO);
    }

    /** 清空当前用户在该智能体下的全部会话（登录用户）。 */
    @DeleteMapping("/{id}/conversations")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> clearConversations(@PathVariable("id") Long agentId) {
        agentService.clearConversations(agentId);
        return Result.ok(null);
    }

    /** 更新会话标题（登录用户）。 */
    @PutMapping("/{id}/conversation/{conversationId}/title")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> updateConversationTitle(
            @PathVariable("id") Long agentId,
            @PathVariable("conversationId") String conversationId,
            @RequestBody @Validated UpdateConversationTitleRequestVO requestVO) {
        agentService.updateConversationTitle(agentId, conversationId, requestVO.getTitle());
        return Result.ok(null);
    }

    /** 分页查询智能体用量明细（管理员）。 */
    @GetMapping("/{id}/usage-detail")
    @LoginRequired
    public PageResult<List<AgentUsageDetailVO>> usageDetail(@PathVariable("id") Long id,
                                                            AgentUsageDetailQueryVO queryVO) {
        return agentAnalyticsService.getUsageDetail(id, queryVO);
    }

    /** 批量删除指定会话（管理员）。 */
    @PostMapping("/{id}/conversations/batch-delete")
    @LoginRequired
    public Result<Void> batchDeleteConversations(
            @PathVariable("id") Long agentId,
            @RequestBody List<String> conversationIds) {
        agentService.batchDeleteConversations(agentId, conversationIds);
        return Result.ok(null);
    }

    // ==================== Chat Conversation Messages ====================

    /** 查询某会话的全部消息记录（登录用户，仅限本人会话）。 */
    @GetMapping("/{id}/conversation/{conversationId}/messages")
    @LoginRequired(role = RoleEnum.USER)
    public Result<List<ConversationRecordVO>> conversationMessages(
            @PathVariable("id") Long agentId,
            @PathVariable("conversationId") String conversationId) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        List<AgentConversationRecordPO> records = conversationRecordMapper.selectList(
                new LambdaQueryWrapper<AgentConversationRecordPO>()
                        .eq(AgentConversationRecordPO::getConversationId, conversationId)
                        .eq(AgentConversationRecordPO::getUserId, userId)
                        .orderByAsc(AgentConversationRecordPO::getCreateDt));
        List<ConversationRecordVO> vos = records.stream().map(r -> ConversationRecordVO.builder()
                .role(r.getRole())
                .content(r.getContent())
                .thinking(r.getThinking())
                .metadata(r.getMetadata())
                .status(r.getStatus())
                .createDt(r.getCreateDt())
                .build()).collect(Collectors.toList());
        return Result.ok(vos);
    }

    // ==================== Agent Market & Subscribe ====================

    /** 查询智能体市场（已发布的公开智能体，登录用户）。 */
    @GetMapping("/market")
    @LoginRequired(role = RoleEnum.USER)
    public Result<List<AgentResponseVO>> market() {
        return Result.ok(userAgentService.getMarket());
    }

    /** 查询当前用户已订阅的智能体（登录用户）。 */
    @GetMapping("/my")
    @LoginRequired(role = RoleEnum.USER)
    public Result<List<AgentResponseVO>> myAgents() {
        return Result.ok(userAgentService.getMyAgents());
    }

    /** 订阅指定智能体（登录用户）。 */
    @PostMapping("/{id}/subscribe")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> subscribe(@PathVariable("id") Long id) {
        userAgentService.subscribe(id);
        return Result.ok(null);
    }

    /** 取消订阅指定智能体（登录用户）。 */
    @DeleteMapping("/{id}/subscribe")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> unsubscribe(@PathVariable("id") Long id) {
        userAgentService.unsubscribe(id);
        return Result.ok(null);
    }
}
