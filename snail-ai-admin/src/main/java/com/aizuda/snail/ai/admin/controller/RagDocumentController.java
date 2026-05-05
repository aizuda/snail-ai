package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.vo.rag.RagDocumentUploadRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadCommitRequestVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadCommitResultVO;
import com.aizuda.snail.ai.admin.vo.rag.UploadPreviewResultVO;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentQueryVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentResponseVO;
import com.aizuda.snail.ai.admin.service.rag.DocumentUploadPreviewService;
import com.aizuda.snail.ai.admin.service.rag.RagDocumentService;
import com.aizuda.snail.ai.features.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class RagDocumentController {

    private final RagDocumentService ragDocumentService;
    private final DocumentUploadPreviewService uploadPreviewService;
    private final ResourceService resourceService;

    /**
     * Upload a file to a knowledge base (legacy 直传，跳过二次确认).
     */
    @PostMapping("/upload")
    @LoginRequired
    public Result<RagDocumentResponseVO> upload(@RequestParam("file") MultipartFile file,
                                                 @Validated RagDocumentUploadRequestVO request) {
        return Result.ok(ragDocumentService.upload(file, request));
    }

    /**
     * 上传预览：批量落临时资源并返回每文件预测决策。
     */
    @PostMapping("/upload/preview")
    @LoginRequired
    public Result<UploadPreviewResultVO> uploadPreview(@RequestParam("files") MultipartFile[] files,
                                                       @Validated RagDocumentUploadRequestVO request) {
        return Result.ok(uploadPreviewService.preview(files, request));
    }

    /**
     * 上传提交：根据 previewToken 与逐行最终决策执行入库。
     */
    @PostMapping("/upload/commit")
    @LoginRequired
    public Result<UploadCommitResultVO> uploadCommit(@RequestBody @Validated UploadCommitRequestVO request) {
        return Result.ok(uploadPreviewService.commit(request));
    }

    /**
     * 取消预览：删除临时资源并失效 token。
     */
    @DeleteMapping("/upload/preview/{token}")
    @LoginRequired
    public Result<Void> cancelUploadPreview(@PathVariable("token") String token) {
        uploadPreviewService.cancel(token);
        return Result.ok(null);
    }

    /**
     * Import document from a URL.
     */
    @PostMapping("/import/url")
    @LoginRequired
    public Result<RagDocumentResponseVO> importFromUrl(@RequestBody @Validated RagDocumentUploadRequestVO request) {
        return Result.ok(ragDocumentService.importFromUrl(request));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        ragDocumentService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/reprocess")
    @LoginRequired
    public Result<Void> reprocess(@PathVariable("id") Long id) {
        ragDocumentService.reprocess(id);
        return Result.ok(null);
    }

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<RagDocumentResponseVO>> page(RagDocumentQueryVO query) {
        return ragDocumentService.page(query);
    }
}
