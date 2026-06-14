package com.aizuda.snail.ai.openapi.controller;

import com.aizuda.snail.ai.common.constants.OpenApiPathConstants;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatStreamEvent;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiChatSyncResponse;
import com.aizuda.snail.ai.openapi.service.OpenApiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * OpenAPI 对话接口
 *
 * @author opensnail
 * @date 2026-04-24
 */
@RestController
@RequiredArgsConstructor
public class OpenApiChatController {

    private final OpenApiChatService openApiChatService;

    @PostMapping(value = OpenApiPathConstants.OPEN_API_AGENT_CHAT, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody @Validated OpenApiChatRequest request) {
        return openApiChatService.chatStream(request)
                .map(this::toServerSentEvent);
    }

    @PostMapping(OpenApiPathConstants.OPEN_API_AGENT_CHAT_SYNC)
    public Result<OpenApiChatSyncResponse> chatSync(@RequestBody @Validated OpenApiChatRequest request) {
        return Result.ok(openApiChatService.chatSync(request));
    }

    private ServerSentEvent<String> toServerSentEvent(OpenApiChatStreamEvent event) {
        return ServerSentEvent.<String>builder(event.getData())
                .event(event.getType())
                .build();
    }
}
