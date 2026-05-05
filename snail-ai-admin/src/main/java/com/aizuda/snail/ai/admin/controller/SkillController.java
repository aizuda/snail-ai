package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.vo.skill.SkillCreateFileRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileContentRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillQueryVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillRenameFileRequestVO;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.skill.SkillCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileTreeNodeVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillResponseVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillUpdateRequestVO;
import com.aizuda.snail.ai.admin.service.skill.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping("/upload")
    @LoginRequired
    public Result<SkillResponseVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(skillService.upload(file));
    }

    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<SkillResponseVO>> page(SkillQueryVO queryVO) {
        return skillService.page(queryVO.getPage(), queryVO.getSize(), queryVO.getKeyword());
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        skillService.delete(id);
        return Result.ok(null);
    }

    /**
     * 下载技能包为 zip 文件（Phase 0）
     */
    @GetMapping("/{id}/download")
    @LoginRequired
    public ResponseEntity<StreamingResponseBody> download(@PathVariable("id") Long id) {
        String filename = "skill-" + id + ".zip";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        StreamingResponseBody body = out -> skillService.writeZipToStream(id, out);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping("/list")
    @LoginRequired
    public Result<List<SkillResponseVO>> listAll() {
        return Result.ok(skillService.listAll());
    }

    @PostMapping
    @LoginRequired
    public Result<SkillResponseVO> create(@RequestBody @Validated SkillCreateRequestVO request) {
        return Result.ok(skillService.createOnline(request));
    }

    @GetMapping("/{id}")
    @LoginRequired
    public Result<SkillResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(skillService.getById(id));
    }

    @PutMapping("/{id}")
    @LoginRequired
    public Result<SkillResponseVO> update(
            @PathVariable("id") Long id, @RequestBody SkillUpdateRequestVO request) {
        return Result.ok(skillService.update(id, request));
    }

    // ==================== 文件管理（在线编辑） ====================

    @GetMapping("/{skillId}/files")
    @LoginRequired
    public Result<SkillFileTreeNodeVO> getFileTree(@PathVariable("skillId") Long skillId) {
        return Result.ok(skillService.buildFileTree(skillId));
    }

    @GetMapping("/{skillId}/files/content")
    @LoginRequired
    public Result<SkillFileContentRequestVO> getFileContent(
            @PathVariable("skillId") Long skillId,
            @RequestParam("path") String path) {
        return Result.ok(skillService.getFileContent(skillId, path));
    }

    @PutMapping("/{skillId}/files/content")
    @LoginRequired
    public Result<Void> saveFileContent(
            @PathVariable("skillId") Long skillId,
            @RequestParam("path") String path,
            @RequestBody SkillFileContentRequestVO body) {
        skillService.saveFileContent(skillId, path, body != null ? body.getContent() : null);
        return Result.ok(null);
    }

    @PostMapping("/{skillId}/files")
    @LoginRequired
    public Result<Void> createFile(
            @PathVariable("skillId") Long skillId,
            @RequestBody SkillCreateFileRequestVO body) {
        skillService.createFile(skillId, body);
        return Result.ok(null);
    }

    @DeleteMapping("/{skillId}/files")
    @LoginRequired
    public Result<Void> deleteFile(
            @PathVariable("skillId") Long skillId,
            @RequestParam("path") String path) {
        skillService.deleteFile(skillId, path);
        return Result.ok(null);
    }

    @PutMapping("/{skillId}/files/rename")
    @LoginRequired
    public Result<Void> renameFile(
            @PathVariable("skillId") Long skillId,
            @RequestBody SkillRenameFileRequestVO body) {
        skillService.renameFile(skillId, body);
        return Result.ok(null);
    }
}
