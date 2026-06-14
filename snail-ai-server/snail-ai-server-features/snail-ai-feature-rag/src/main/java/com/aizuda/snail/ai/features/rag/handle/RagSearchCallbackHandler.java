package com.aizuda.snail.ai.features.rag.handle;

import com.aizuda.snail.ai.common.dto.rag.RagSearchRequest;
import com.aizuda.snail.ai.common.dto.rag.RagSearchResponse;
import com.aizuda.snail.ai.common.grpc.auto.GrpcSnailAiResult;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;
import com.aizuda.snail.ai.common.grpc.handler.GrpcHandlerRequest;
import com.aizuda.snail.ai.common.grpc.handler.GrpcRequestHandler;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchResponseDTO;
import com.aizuda.snail.ai.features.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 回调：RAG 知识库搜索
 * <p>
 * 客户端 KnowledgeSearchTool 通过 gRPC 调用此处理器执行检索，
 * 观测责任由客户端承担。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchCallbackHandler implements GrpcRequestHandler {

    private final RagSearchService ragSearchService;

    @Override
    public boolean supports(String uri) {
        return UriConstants.CALLBACK_RAG_SEARCH.equals(uri);
    }

    @Override
    public GrpcSnailAiResult handle(GrpcHandlerRequest request) {
        try {
            RagSearchRequest ragSearchRequest = JsonUtil.parseObject(request.getBody(), RagSearchRequest.class);
            Long ragId = ragSearchRequest.getRagId();
            String query = ragSearchRequest.getQuery();
            if (ragId == null || query == null || query.isBlank()) {
                return buildError("ragId and query are required");
            }

            RagSearchRequestDTO req = new RagSearchRequestDTO();
            req.setRagId(ragId);
            req.setQuery(query);

            RagSearchResponseDTO resp = ragSearchService.search(req);
            RagSearchResponse ragSearchResponse = new RagSearchResponse();
            ragSearchResponse.setResults(resp.getResults());
            return GrpcSnailAiResult.newBuilder()
                    .setStatus(1).setMessage("OK")
                    .setData(JsonUtil.toJsonString(ragSearchResponse))
                    .build();
        } catch (Exception e) {
            log.error("Callback RAG search failed", e);
            return buildError(e.getMessage());
        }
    }

    private GrpcSnailAiResult buildError(String msg) {
        return GrpcSnailAiResult.newBuilder().setStatus(0).setMessage(msg).build();
    }
}
