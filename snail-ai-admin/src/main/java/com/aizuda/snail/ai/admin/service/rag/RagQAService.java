package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.model.ModelFactory;
import com.aizuda.snail.ai.model.model.chat.ChatModel;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.admin.vo.rag.RagQARequestVO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQAService {

    private final RagSearchService ragSearchService;
    private final ModelFactory modelFactory;
    private final RagMapper knowledgeMapper;

    /**
     * 流式问答：从 DB 读取知识库配置 → 检索 → 组装 prompt → 调用 LLM 流式输出
     */
    public void qaStream(RagQARequestVO request, ResponseBodyEmitter emitter) {
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

            // 1. 检索（直接使用 SearchParams，无需转换）
            RagConfigDO searchConfigDO = RagConfigDO.builder()
                    .searchParams(searchParams)
                    .modelParams(mp)
                    .build();
            RagSearchRequestDTO searchRequest = new RagSearchRequestDTO();
            searchRequest.setRagId(request.getRagId());
            searchRequest.setQuery(request.getQuery());
            List<SearchResult> searchResults = new ArrayList<>(
                    ragSearchService.search(searchRequest, searchConfigDO).getResults());


            // 4. 组装 prompt
            String documentsText = buildDocumentsText(searchResults);
            String systemPrompt = buildSystemPrompt(mp.getPrompt(), documentsText);

            // 5. 流式调用 LLM
            ChatModel chatModel = (ChatModel) modelFactory.getModel(mp.getModelId());
            chatModel.chatStreamModel(new ChatModel.ChatStreamModelDTO(
                    mp.getModelId(),
                    request.getQuery(),
                    systemPrompt,
                    chunk -> {
                        try {
                            emitter.send(chunk, MediaType.TEXT_PLAIN);
                        } catch (IOException e) {
                            log.error("写入流失败", e);
                        }
                    },
                    emitter::complete,
                    emitter::completeWithError
            ));

        } catch (Exception e) {
            log.error("RAG QA stream error: query='{}', ragId={}", request.getQuery(), request.getRagId(), e);
            try {
                emitter.send("Error: " + e.getMessage(), MediaType.TEXT_PLAIN);
            } catch (IOException ex) {
                log.error("写入错误信息失败", ex);
            }
            emitter.completeWithError(e);
        }
    }

    private String buildDocumentsText(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "（未找到相关参考资料）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("[%d] %s\n\n", i + 1, results.get(i).getContent()));
        }
        return sb.toString().trim();
    }

    private String buildSystemPrompt(String promptTemplate, String documentsText) {
        if (!StringUtils.hasText(promptTemplate)) {
            return "请根据以下参考资料回答用户的问题：\n\n" + documentsText;
        }
        return promptTemplate.replace("<Documents>", documentsText);
    }
}
