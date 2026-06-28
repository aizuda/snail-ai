package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.vo.rag.RagChunkCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkUpdateRequestVO;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkQueryVO;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkResponseVO;
import com.aizuda.snail.ai.admin.service.rag.RagDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 文档切块（chunk）管理接口：切块的分页、详情、增删改。
 * <p>
 * 全部端点均需 ADMIN 角色。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/chunk")
@RequiredArgsConstructor
public class RagChunkController {

    private final RagDocumentService ragDocumentService;

    /** 分页查询文档切块（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<RagChunkResponseVO>> page(RagChunkQueryVO query) {
        return ragDocumentService.chunkPage(query);
    }

    /** 查询切块详情（管理员）。 */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<RagChunkResponseVO> detail(@PathVariable("id") Long id) {
        return Result.ok(ragDocumentService.getChunkDetail(id));
    }

    /** 新建切块（管理员，会同步向量库）。 */
    @PostMapping
    @LoginRequired
    public Result<RagChunkResponseVO> create(@RequestBody RagChunkCreateRequestVO request) {
        return Result.ok(ragDocumentService.createChunk(request));
    }

    /** 更新切块内容（管理员，会同步向量库）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<RagChunkResponseVO> update(@PathVariable("id") Long id, @RequestBody RagChunkUpdateRequestVO request) {
        return Result.ok(ragDocumentService.updateChunk(id, request));
    }

    /** 删除切块（管理员，会同步向量库）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        ragDocumentService.deleteChunk(id);
        return Result.ok(null);
    }
}
