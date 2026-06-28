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
import com.aizuda.snail.ai.admin.stream.RagQaStreamHttpAdapter;
import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchResponseDTO;
import com.aizuda.snail.ai.features.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 知识库（RAG）管理与检索接口：知识库 CRUD / 配置、向量检索与流式问答。
 * <p>
 * 全部端点均需 ADMIN 角色。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagSearchController {

    private final RagSearchService ragSearchService;
    private final RagQAService ragQAService;
    private final KnowledgeService knowledgeService;
    private final RagQaStreamHttpAdapter ragQaStreamHttpAdapter;

    /** 创建知识库（管理员）。 */
    @PostMapping
    @LoginRequired
    public Result<KnowledgeResponseVO> create(@RequestBody @Validated KnowledgeRequestVO request) {
        return Result.ok(knowledgeService.create(request));
    }

    /** 更新知识库基本信息（管理员）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<KnowledgeResponseVO> update(@PathVariable("id") Long id,
                                              @RequestBody @Validated KnowledgeRequestVO request) {
        return Result.ok(knowledgeService.update(id, request));
    }

    /** 更新知识库检索 / 切块配置（管理员）。 */
    @PutMapping("/{id}/config")
    @LoginRequired
    public Result<Void> updateConfig(@PathVariable("id") Long id,
                                     @RequestBody KnowledgeConfigRequestVO config) {
        knowledgeService.updateConfig(id, config);
        return Result.ok(null);
    }

    /** 删除知识库（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        knowledgeService.delete(id);
        return Result.ok(null);
    }

    /** 按 ID 查询知识库详情（管理员）。 */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<KnowledgeResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.getById(id));
    }

    /** 分页查询知识库列表（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<KnowledgeResponseVO>> page(KnowledgeQueryVO query) {
        return knowledgeService.page(query);
    }

    /** 在指定知识库内执行向量检索（管理员，debug 可返回检索指标）。 */
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

    /** 基于知识库的流式问答（管理员，SSE 流）。 */
    @PostMapping(value = "/qa/stream", produces = RagQaStreamHttpAdapter.MEDIA_TYPE)
    @LoginRequired
    public Flux<String> qaStream(@RequestBody @Validated RagQARequestVO request) {
        return ragQaStreamHttpAdapter.encode(ragQAService.qaStream(request));
    }
}
