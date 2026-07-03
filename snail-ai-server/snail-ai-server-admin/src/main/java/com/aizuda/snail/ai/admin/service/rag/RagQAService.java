package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.dto.RagQaStreamEvent;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.admin.vo.rag.RagQARequestVO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库问答服务，基于检索到的参考资料拼装提示词并调用模型进行（流式）问答。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagQAService {

    private static final String EMPTY_DOCUMENTS_TEXT = "（未找到相关参考资料）";
    private static final String DOCUMENTS_PLACEHOLDER = "<Documents>";
    private static final String DEFAULT_SYSTEM_PROMPT_PREFIX = "请根据以下参考资料回答用户的问题：\n\n";

    private final RagSearchService ragSearchService;
    private final ModelRuntimeHandler modelRuntimeHandler;
    private final RagMapper knowledgeMapper;

    /**
     * 流式问答：从 DB 读取知识库配置 → 检索 → 组装 prompt → 调用 LLM 流式输出
     *
     * @param request 知识库问答请求（知识库 ID 与用户问题）
     * @return 问答过程的流式事件（文本块/结束/错误）
     */
    public Flux<RagQaStreamEvent> qaStream(RagQARequestVO request) {
        return Flux.create(sink -> doQaStream(request, sink), FluxSink.OverflowStrategy.BUFFER);
    }

    private void doQaStream(RagQARequestVO request, FluxSink<RagQaStreamEvent> sink) {
        log.debug("知识库问答开始 ragId={} query={}", request.getRagId(), request.getQuery());
        try {
            RagPO knowledge = knowledgeMapper.selectById(request.getRagId());
            if (knowledge == null) {
                throw new SnailAiException("Knowledge not found: " + request.getRagId());
            }

            // 读取知识库 DB 配置
            RagConfigDO configDO = StrUtil.isNotBlank(knowledge.getConfig())
                    ? JsonUtil.parseObject(knowledge.getConfig(), RagConfigDO.class)
                    : new RagConfigDO();
            if (configDO == null) {
                configDO = new RagConfigDO();
            }
            RagConfigDO.SearchParams searchParams = configDO.getSearchParams() != null
                    ? configDO.getSearchParams() : new RagConfigDO.SearchParams();
            RagConfigDO.ModelParams mp = configDO.getModelParams() != null
                    ? configDO.getModelParams() : new RagConfigDO.ModelParams();

            if (mp.getModelId() == null) {
                throw new IllegalStateException("知识库未配置问答模型，请先在配置页面设置");
            }

            // 1. 检索
            RagConfigDO searchConfigDO = RagConfigDO.builder()
                    .searchParams(searchParams)
                    .modelParams(mp)
                    .build();
            RagSearchRequestDTO searchRequest = new RagSearchRequestDTO();
            searchRequest.setRagId(request.getRagId());
            searchRequest.setQuery(request.getQuery());
            List<SearchResult> searchResults = new ArrayList<>(
                    ragSearchService.search(searchRequest, searchConfigDO).getResults());
            log.debug("知识库问答检索完成 ragId={} 命中={} 条", request.getRagId(), searchResults.size());

            // 2. 组装 prompt
            String documentsText = buildDocumentsText(searchResults);
            String systemPrompt = buildSystemPrompt(mp.getPrompt(), documentsText);

            // 3. 流式调用 LLM
            Disposable subscription = modelRuntimeHandler.chatStream(new ModelRuntimeHandler.ChatStreamRequest(
                    mp.getModelId(),
                    request.getQuery(),
                    systemPrompt,
                    chunk -> emit(sink, RagQaStreamEvent.text(chunk)),
                    () -> complete(sink, searchResults),
                    error -> emitError(sink, error)
            ));
            sink.onCancel(subscription);

        } catch (Exception e) {
            log.error("RAG QA stream error: query='{}', ragId={}", request.getQuery(), request.getRagId(), e);
            emitError(sink, e);
        }
    }

    private String buildDocumentsText(List<SearchResult> results) {
        if (results.isEmpty()) {
            return EMPTY_DOCUMENTS_TEXT;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("[%d] %s\n\n", i + 1, results.get(i).getContent()));
        }
        return sb.toString().trim();
    }

    private String buildSystemPrompt(String promptTemplate, String documentsText) {
        if (StrUtil.isBlank(promptTemplate)) {
            return DEFAULT_SYSTEM_PROMPT_PREFIX + documentsText;
        }
        return promptTemplate.replace(DOCUMENTS_PLACEHOLDER, documentsText);
    }

    private void emit(FluxSink<RagQaStreamEvent> sink, RagQaStreamEvent event) {
        if (sink.isCancelled()) {
            return;
        }
        sink.next(event);
    }

    private void complete(FluxSink<RagQaStreamEvent> sink, List<SearchResult> references) {
        if (sink.isCancelled()) {
            return;
        }
        sink.next(RagQaStreamEvent.done(references));
        sink.complete();
    }

    private void emitError(FluxSink<RagQaStreamEvent> sink, Throwable error) {
        if (sink.isCancelled()) {
            return;
        }
        String message = error != null && error.getMessage() != null
                ? error.getMessage()
                : "RAG QA stream error";
        sink.next(RagQaStreamEvent.error(message));
        sink.complete();
    }
}
