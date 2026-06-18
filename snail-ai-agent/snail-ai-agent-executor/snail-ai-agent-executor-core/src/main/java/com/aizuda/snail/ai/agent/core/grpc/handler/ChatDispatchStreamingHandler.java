package com.aizuda.snail.ai.agent.core.grpc.handler;

import com.aizuda.snail.ai.agent.core.executor.ClientChatExecutor;
import com.aizuda.snail.ai.agent.core.runtime.ChatSessionRequest;
import com.aizuda.snail.ai.agent.core.runtime.ChatSessionRuntime;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcStreamingRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端 Chat 分发（流式）：{@link UriConstants#CHAT_DISPATCH}
 */
@Slf4j
@RequiredArgsConstructor
public class ChatDispatchStreamingHandler implements GrpcStreamingRequestHandler {

    private static final int REQUEST_STATUS_SUCCESS = 1;
    private static final int REQUEST_STATUS_FAILED = 0;
    private static final String ERROR_CODE_LLM_FAILED = "LLM_FAILED";

    private final ChatSessionRuntime chatSessionRuntime;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CHAT_DISPATCH.equals(uri);
    }

    @Override
    public void handle(GrpcHandlerRequest request,
                       StreamObserver<GrpcSnailAiResult> observer) {
        long reqId = request.getReqId();
        ChatDispatchRequest dispatchRequest = parseDispatchRequest(request.getBody());
        String requestId = dispatchRequest.getRequestId();
        String sid = dispatchRequest.getSid();

        log.info("Chat dispatch received: requestId={}, sid={}", requestId, sid);

        chatSessionRuntime.execute(ChatSessionRequest.builder()
                .dispatchRequest(dispatchRequest)
                .textConsumer(text -> handleTextChunk(reqId, sid, text, observer))
                .thinkingConsumer(thinking -> handleThinkingChunk(reqId, sid, thinking, observer))
                .completionConsumer(completion -> handleCompletion(reqId, requestId, sid, completion, observer))
                .errorConsumer(error -> handleError(reqId, sid, error, observer))
                .build());
    }

    private ChatDispatchRequest parseDispatchRequest(String body) {
        return JsonUtil.parseObject(body, ChatDispatchRequest.class);
    }

    private void handleTextChunk(long reqId, String sid, String text, StreamObserver<GrpcSnailAiResult> observer) {
        try {
            ChatStreamResponse response = ChatStreamResponse.text(sid, text);
            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_SUCCESS)
                    .setData(data)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send chunk", e);
        }
    }

    private void handleThinkingChunk(long reqId, String sid, String text, StreamObserver<GrpcSnailAiResult> observer) {
        try {
            ChatStreamResponse response = ChatStreamResponse.thinking(sid, text);
            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_SUCCESS)
                    .setData(data)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send thinking chunk", e);
        }
    }

    private void handleCompletion(ChatCompletionContext context) {
        try {
            log.info("handleCompletion: promptTokens={}, completionTokens={}, cacheTokens={}, durationMs={}",
                    context.completion().promptTokens(), context.completion().completionTokens(),
                    context.completion().cacheTokens(), context.completion().durationMs());
            ChatStreamResponse response = ChatStreamResponse.completion(
                    context.sid(),
                    context.completion().fullText(),
                    context.completion().fullThinking(),
                    context.completion().promptTokens(),
                    context.completion().completionTokens(),
                    context.completion().cacheTokens(),
                    context.completion().durationMs());

            String data = JsonUtil.toJsonString(response);
            context.observer().onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(context.reqId())
                    .setStatus(REQUEST_STATUS_SUCCESS)
                    .setData(data)
                    .build());
            context.observer().onCompleted();
            log.info("Chat completed: requestId={}, sid={}, duration={}ms",
                    context.requestId(), context.sid(), context.completion().durationMs());
        } catch (Exception e) {
            log.warn("Failed to send completion", e);
        }
    }

    private void handleCompletion(long reqId, String requestId, String sid,
                                  ClientChatExecutor.ChatCompletionResult completion,
                                  StreamObserver<GrpcSnailAiResult> observer) {
        handleCompletion(new ChatCompletionContext(reqId, requestId, sid, completion, observer));
    }

    private void handleError(long reqId, String sid, Throwable error,
                             StreamObserver<GrpcSnailAiResult> observer) {
        try {
            String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error";
            ChatStreamResponse response = ChatStreamResponse.error(sid, ERROR_CODE_LLM_FAILED, errorMessage);

            String data = JsonUtil.toJsonString(response);
            observer.onNext(GrpcSnailAiResult.newBuilder()
                    .setReqId(reqId)
                    .setStatus(REQUEST_STATUS_FAILED)
                    .setData(data)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.warn("Failed to send error", e);
        }
    }

    private record ChatCompletionContext(
            long reqId,
            String requestId,
            String sid,
            ClientChatExecutor.ChatCompletionResult completion,
            StreamObserver<GrpcSnailAiResult> observer) {
    }
}
