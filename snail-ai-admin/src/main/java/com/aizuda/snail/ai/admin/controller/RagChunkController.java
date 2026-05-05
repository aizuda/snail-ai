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

@RestController
@RequestMapping("/chunk")
@RequiredArgsConstructor
public class RagChunkController {

    private final RagDocumentService ragDocumentService;

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<RagChunkResponseVO>> page(RagChunkQueryVO query) {
        return ragDocumentService.chunkPage(query);
    }

    @GetMapping("/{id}")
    @LoginRequired
    public Result<RagChunkResponseVO> detail(@PathVariable("id") Long id) {
        return Result.ok(ragDocumentService.getChunkDetail(id));
    }

    @PostMapping
    @LoginRequired
    public Result<RagChunkResponseVO> create(@RequestBody RagChunkCreateRequestVO request) {
        return Result.ok(ragDocumentService.createChunk(request));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<RagChunkResponseVO> update(@PathVariable("id") Long id, @RequestBody RagChunkUpdateRequestVO request) {
        return Result.ok(ragDocumentService.updateChunk(id, request));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        ragDocumentService.deleteChunk(id);
        return Result.ok(null);
    }
}
