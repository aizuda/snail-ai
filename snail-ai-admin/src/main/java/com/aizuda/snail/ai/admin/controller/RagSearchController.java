package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.service.knowledge.KnowledgeService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.knowledge.KnowledgeConfigRequestVO;
import com.aizuda.snail.ai.admin.vo.knowledge.KnowledgeQueryVO;
import com.aizuda.snail.ai.admin.vo.knowledge.KnowledgeRequestVO;
import com.aizuda.snail.ai.admin.vo.knowledge.KnowledgeResponseVO;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.rag.RagQARequestVO;
import com.aizuda.snail.ai.admin.vo.rag.RagSearchRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.RagSearchResponseVO;
import com.aizuda.snail.ai.admin.service.rag.RagQAService;
import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchResponseDTO;
import com.aizuda.snail.ai.features.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagSearchController {

    private final RagSearchService ragSearchService;
    private final RagQAService ragQAService;
    private final KnowledgeService knowledgeService;

    @PostMapping
    @LoginRequired
    public Result<KnowledgeResponseVO> create(@RequestBody @Validated KnowledgeRequestVO request) {
        return Result.ok(knowledgeService.create(request));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<KnowledgeResponseVO> update(@PathVariable("id") Long id,
                                              @RequestBody @Validated KnowledgeRequestVO request) {
        return Result.ok(knowledgeService.update(id, request));
    }

    @PutMapping("/{id}/config")
    @LoginRequired
    public Result<Void> updateConfig(@PathVariable("id") Long id,
                                     @RequestBody KnowledgeConfigRequestVO config) {
        knowledgeService.updateConfig(id, config);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        knowledgeService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/{id}")
    @LoginRequired
    public Result<KnowledgeResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.getById(id));
    }

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<KnowledgeResponseVO>> page(KnowledgeQueryVO query) {
        return knowledgeService.page(query);
    }

    @PostMapping("/search")
    @LoginRequired
    public Result<RagSearchResponseVO> search(@RequestBody @Validated RagSearchRequestVO request) {
        RagSearchRequestDTO dto = new RagSearchRequestDTO();
        dto.setRagId(request.getRagId());
        dto.setQuery(request.getQuery());
        dto.setDebug(request.getDebug());
        RagSearchResponseDTO resp = ragSearchService.search(dto);
        return Result.ok(RagSearchResponseVO.builder()
                .results(resp.getResults())
                .metrics(resp.getMetrics())
                .build());
    }

    @PostMapping(value = "/qa/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    @LoginRequired
    public ResponseBodyEmitter qaStream(@RequestBody @Validated RagQARequestVO request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L); // 无超时
        ragQAService.qaStream(request, emitter);
        return emitter;
    }
}
