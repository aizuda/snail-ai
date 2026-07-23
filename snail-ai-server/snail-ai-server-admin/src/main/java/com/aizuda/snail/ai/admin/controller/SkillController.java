package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateResponseVO;
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

/**
 * 技能（Skill）管理接口：技能包上传 / 下载、CRUD 与在线文件编辑。
 * <p>
 * 全部端点均需 ADMIN 角色（{@code @LoginRequired} 默认角色）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    /** 上传技能包（zip）并解析入库（管理员）。 */
    @PostMapping("/upload")
    @LoginRequired
    public Result<SkillResponseVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(skillService.upload(file));
    }

    /** 分页查询技能列表（管理员）。 */
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<SkillResponseVO>> page(SkillQueryVO queryVO) {
        return skillService.page(queryVO.getPage(), queryVO.getSize(), queryVO.getKeyword());
    }

    /** 删除技能（管理员）。 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable("id") Long id) {
        skillService.delete(id);
        return Result.ok(null);
    }

    /** 将技能包导出为 zip 文件下载（管理员，流式响应）。 */
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

    /** 查询全部技能（不分页，管理员）。 */
    @GetMapping("/list")
    @LoginRequired
    public Result<List<SkillResponseVO>> listAll() {
        return Result.ok(skillService.listAll());
    }

    /** 在线创建技能（管理员）。 */
    @PostMapping
    @LoginRequired
    public Result<SkillResponseVO> create(@RequestBody @Validated SkillCreateRequestVO request) {
        return Result.ok(skillService.createOnline(request));
    }

    /** 按 ID 查询技能详情（管理员）。 */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<SkillResponseVO> getById(@PathVariable("id") Long id) {
        return Result.ok(skillService.getById(id));
    }

    /** 更新技能基本信息（管理员）。 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<SkillResponseVO> update(
            @PathVariable("id") Long id, @RequestBody SkillUpdateRequestVO request) {
        return Result.ok(skillService.update(id, request));
    }

    /** 根据需求说明生成完整 SKILL.md 内容，不创建版本。 */
    @PostMapping("/ai/generate")
    @LoginRequired
    public Result<SkillAiGenerateResponseVO> generateDraft(
            @RequestBody @Validated SkillAiGenerateRequestVO request) {
        return Result.ok(skillService.generateDraft(request));
    }

    /** 基于当前 SKILL.md 内容优化指定技能，不创建版本。 */
    @PostMapping("/{id}/ai/optimize")
    @LoginRequired
    public Result<SkillAiGenerateResponseVO> optimizeDraft(
            @PathVariable("id") Long id,
            @RequestBody @Validated SkillAiGenerateRequestVO request) {
        return Result.ok(skillService.optimizeDraft(id, request));
    }

    // ==================== 文件管理（在线编辑） ====================

    /** 查询技能内文件树（管理员）。 */
    @GetMapping("/{skillId}/files")
    @LoginRequired
    public Result<SkillFileTreeNodeVO> getFileTree(@PathVariable("skillId") Long skillId) {
        return Result.ok(skillService.buildFileTree(skillId));
    }

    /** 读取技能内指定文件内容（管理员）。 */
    @GetMapping("/{skillId}/files/content")
    @LoginRequired
    public Result<SkillFileContentRequestVO> getFileContent(
            @PathVariable("skillId") Long skillId,
            @RequestParam("path") String path) {
        return Result.ok(skillService.getFileContent(skillId, path));
    }

    /** 保存技能内指定文件内容（管理员）。 */
    @PutMapping("/{skillId}/files/content")
    @LoginRequired
    public Result<Void> saveFileContent(
            @PathVariable("skillId") Long skillId,
            @RequestParam("path") String path,
            @RequestBody SkillFileContentRequestVO body) {
        skillService.saveFileContent(skillId, path, body != null ? body.getContent() : null);
        return Result.ok(null);
    }

    /** 在技能内新建文件（管理员）。 */
    @PostMapping("/{skillId}/files")
    @LoginRequired
    public Result<Void> createFile(
            @PathVariable("skillId") Long skillId,
            @RequestBody SkillCreateFileRequestVO body) {
        skillService.createFile(skillId, body);
        return Result.ok(null);
    }

    /** 删除技能内指定文件（管理员）。 */
    @DeleteMapping("/{skillId}/files")
    @LoginRequired
    public Result<Void> deleteFile(
            @PathVariable("skillId") Long skillId,
            @RequestParam("path") String path) {
        skillService.deleteFile(skillId, path);
        return Result.ok(null);
    }

    /** 重命名技能内文件（管理员）。 */
    @PutMapping("/{skillId}/files/rename")
    @LoginRequired
    public Result<Void> renameFile(
            @PathVariable("skillId") Long skillId,
            @RequestBody SkillRenameFileRequestVO body) {
        skillService.renameFile(skillId, body);
        return Result.ok(null);
    }
}
