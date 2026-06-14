package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationIdentityRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiConversationQueryRequest;
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
 * @date 2026-04-24
 */
@RestController
@RequiredArgsConstructor
@Validated
public class OpenApiConversationController {

    private final OpenApiConversationService openApiConversationService;

    @GetMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS)
    public PageResult<List<OpenApiConversationVO>> listConversations(
            @Validated OpenApiConversationQueryRequest request) {
        return openApiConversationService.listConversations(request);
    }

    @PostMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS)
    public Result<OpenApiConversationVO> createConversation(
            @RequestBody @Validated OpenApiCreateConversationRequest request) {
        return Result.ok(openApiConversationService.createConversation(request));
    }

    @DeleteMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATIONS)
    public Result<Void> deleteConversation(
            @Validated OpenApiConversationIdentityRequest request) {
        openApiConversationService.deleteConversation(request);
        return Result.ok(null);
    }

    @GetMapping(OpenApiPathConstants.OPEN_API_AGENT_CONVERSATION_MESSAGES)
    public Result<List<OpenApiMessageVO>> getMessages(
            @Validated OpenApiConversationIdentityRequest request) {
        return Result.ok(openApiConversationService.getMessages(request));
    }
}
