package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.dto.AgentChatCommand;
import com.aizuda.snail.ai.admin.service.agent.*;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.agent.*;
import com.aizuda.snail.ai.admin.service.mcp.McpServerService;
import com.aizuda.snail.ai.admin.vo.mcp.McpServerResponseVO;
import com.aizuda.snail.ai.admin.vo.memory.ConversationSummaryVO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentConversationRecordMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentConversationRecordPO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentCreateService agentCreateService;
    private final McpServerService mcpServerService;
    private final UserAgentService userAgentService;
    private final AgentConversationRecordMapper conversationRecordMapper;
    private final AgentChatService agentChatService;
    private final AgentAnalyticsService agentAnalyticsService;

    // ==================== CRUD ====================

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<AgentResponseVO>> page(AgentQueryVO query) {
        return agentService.page(query);
    }

    @GetMapping("/{id}")
    @LoginRequired(role = RoleEnum.USER)
    public Result<AgentResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(agentService.getById(id));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<AgentResponseVO> update(@PathVariable("id") Long id,
                                           @RequestBody AgentRequestVO request) {
        return Result.ok(agentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentService.delete(id);
        return Result.ok(null);
    }

    // ==================== Chat Models ====================

    @GetMapping("/chat-models")
    @LoginRequired
    public Result<List<Map<String, Object>>> chatModels() {
        return Result.ok(agentService.getChatModels());
    }

    // ==================== AI Create ====================

    @PostMapping(value = "/create/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    @LoginRequired
    public ResponseBodyEmitter createStream(@RequestBody @Validated AgentCreateRequestVO request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        agentCreateService.createByDescriptionStream(request.getDescription(), emitter);
        return emitter;
    }

    @PostMapping
    @LoginRequired
    public Result<AgentResponseVO> create(@RequestBody @Validated AgentRequestVO request) {
        return Result.ok(agentService.create(request));
    }

    // ==================== Chat (SSE) ====================

    @PostMapping(value = "/{id}/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    @LoginRequired(role = RoleEnum.USER)
    public ResponseBodyEmitter chat(@PathVariable("id") Long id,
                                    @RequestBody @Validated AgentChatRequestVO request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        agentChatService.chat(AgentChatCommand.builder()
                .agentId(id)
                .conversationId(request.getConversationId())
                .content(request.getContent())
                .disabledMcpServerIds(request.getDisabledMcpServerIds())
                .disabledSkillIds(request.getDisabledSkillIds())
                .emitter(emitter)
                .build());
        return emitter;
    }

    // ==================== MCP Servers ====================

    @GetMapping("/{id}/mcp-servers")
    @LoginRequired
    public Result<List<McpServerResponseVO>> getMcpServers(@PathVariable("id") Long id) {
        return Result.ok(mcpServerService.getByAgentId(id));
    }

    @PutMapping("/{id}/mcp-servers")
    @LoginRequired
    public Result<Void> updateMcpServers(@PathVariable("id") Long id,
                                          @RequestBody AgentMcpServersUpdateRequestVO requestVO) {
        mcpServerService.updateAgentMcpServers(id, requestVO.getMcpServerIds());
        return Result.ok(null);
    }

    // ==================== Analytics ====================
    // Analytics endpoints have been simplified
    @GetMapping("/{id}/analytics")
    @LoginRequired
    public Result<AgentAnalyticsVO> analytics(@PathVariable("id") Long id, AgentAnalyticsQueryVO queryVO) {
        return Result.ok(agentAnalyticsService.getAnalytics(id, queryVO));
    }


    @DeleteMapping("/{id}/conversation/{conversationId}")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> deleteConversation(
            @PathVariable("id") Long agentId,
            @PathVariable("conversationId") String conversationId) {
        agentService.deleteConversation(agentId, conversationId);
        return Result.ok(null);
    }

    @GetMapping("/{id}/conversations")
    @LoginRequired(role = RoleEnum.USER)
    public PageResult<List<ConversationSummaryVO>> conversations(@PathVariable("id") Long id,
            AgentConversationQueryVO queryVO) {
        return agentService.listConversations(id, queryVO);
    }

    @GetMapping("/{id}/usage-detail")
    @LoginRequired
    public PageResult<List<AgentUsageDetailVO>> usageDetail(@PathVariable("id") Long id,
                                                            AgentUsageDetailQueryVO queryVO) {
        return agentAnalyticsService.getUsageDetail(id, queryVO);
    }


    @PostMapping("/{id}/conversations/batch-delete")
    @LoginRequired
    public Result<Void> batchDeleteConversations(
            @PathVariable("id") Long agentId,
            @RequestBody List<String> conversationIds) {
        agentService.batchDeleteConversations(agentId, conversationIds);
        return Result.ok(null);
    }

    // ==================== Chat Conversation Messages ====================

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
                .status(r.getStatus())
                .createDt(r.getCreateDt())
                .build()).collect(Collectors.toList());
        return Result.ok(vos);
    }

    // ==================== Agent Market & Subscribe ====================

    @GetMapping("/market")
    @LoginRequired(role = RoleEnum.USER)
    public Result<List<AgentResponseVO>> market() {
        return Result.ok(userAgentService.getMarket());
    }

    @GetMapping("/my")
    @LoginRequired(role = RoleEnum.USER)
    public Result<List<AgentResponseVO>> myAgents() {
        return Result.ok(userAgentService.getMyAgents());
    }

    @PostMapping("/{id}/subscribe")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> subscribe(@PathVariable("id") Long id) {
        userAgentService.subscribe(id);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}/subscribe")
    @LoginRequired(role = RoleEnum.USER)
    public Result<Void> unsubscribe(@PathVariable("id") Long id) {
        userAgentService.unsubscribe(id);
        return Result.ok(null);
    }

    // ==================== Score ====================
    // Score endpoints have been simplified

    // ==================== Trace Bookmark ====================
    // Trace bookmark endpoints have been simplified
}
