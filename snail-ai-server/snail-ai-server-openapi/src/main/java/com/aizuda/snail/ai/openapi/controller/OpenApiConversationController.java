package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationClearRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationUpdateTitleRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationVO;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiCreateConversationRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiMessageVO;
import com.aizuda.snail.ai.openapi.service.OpenApiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenAPI 会话管理接口
 *
 * @author opensnail
 * @since 2026-04-24
 */
@RestController
@RequiredArgsConstructor
@Validated
public class OpenApiConversationController {

    private final OpenApiConversationService openApiConversationService;

    /**
     * 分页查询会话列表
     *
     * @param request 会话分页查询请求（智能体ID、openId、分页参数）
     * @return 会话分页结果
     */
    @GetMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS)
    public PageResult<List<OpenApiConversationVO>> listConversations(
            @Validated OpenApiConversationQueryRequest request) {
        return openApiConversationService.listConversations(request);
    }

    /**
     * 创建会话
     *
     * @param request 创建会话请求（智能体ID、openId、标题）
     * @return 新建的会话
     */
    @PostMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS)
    public Result<OpenApiConversationVO> createConversation(
            @RequestBody @Validated OpenApiCreateConversationRequest request) {
        return Result.ok(openApiConversationService.createConversation(request));
    }

    /**
     * 删除指定会话
     *
     * @param request 会话标识请求（智能体ID、会话ID、openId）
     * @return 统一成功响应
     */
    @DeleteMapping(value = OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS, params = "conversationId")
    public Result<Void> deleteConversation(
            @Validated OpenApiConversationIdentityRequest request) {
        openApiConversationService.deleteConversation(request);
        return Result.ok(null);
    }

    /**
     * 清空指定智能体下当前用户的全部会话
     *
     * @param request 清空会话请求（智能体ID、openId）
     * @return 统一成功响应
     */
    @DeleteMapping(value = OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS, params = "!conversationId")
    public Result<Void> clearConversations(
            @Validated OpenApiConversationClearRequest request) {
        openApiConversationService.clearConversations(request);
        return Result.ok(null);
    }

    /**
     * 更新会话标题
     *
     * @param request 更新标题请求（智能体ID、会话ID、openId、标题）
     * @return 统一成功响应
     */
    @PutMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS)
    public Result<Void> updateConversationTitle(
            @RequestBody @Validated OpenApiConversationUpdateTitleRequest request) {
        openApiConversationService.updateConversationTitle(request);
        return Result.ok(null);
    }

    /**
     * 获取会话内消息列表
     *
     * @param request 会话标识请求（智能体ID、会话ID、openId）
     * @return 消息列表
     */
    @GetMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATION_MESSAGES)
    public Result<List<OpenApiMessageVO>> getMessages(
            @Validated OpenApiConversationIdentityRequest request) {
        return Result.ok(openApiConversationService.getMessages(request));
    }
}
