package com.aizuda.snail.ai.admin.service.skill;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillAiGenerateResponseVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillCreateFileRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileContentRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileTreeNodeVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillRenameFileRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillResponseVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillUpdateRequestVO;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentSkillMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentSkillPO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.aizuda.snail.ai.common.constants.SystemConstants.SKILL_MD;

/**
 * 技能管理门面服务，负责技能的增删改查、智能体技能关联与在线文件编辑的流程编排：
 * zip 解压/打包委托 {@link SkillPackageService}，支撑文件持久化委托 {@link SkillFileService}，
 * SKILL.md 解析与重建委托 {@link SkillMarkdownParser}；事务边界统一由本类 public 方法声明。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillMapper skillMapper;
    private final AgentSkillMapper agentSkillMapper;
    private final SkillPackageService skillPackageService;
    private final SkillFileService skillFileService;
    private final SkillMarkdownParser skillMarkdownParser;
    private final SkillAiContentService skillAiContentService;

    /**
     * 上传 Skill zip 包
     *
     * @param file 待上传的 .zip 技能包（须含 SKILL.md）
     * @return 创建成功的技能响应 VO
     */
    public SkillResponseVO upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new SnailAiException("仅支持上传 .zip 格式的 Skill 包");
        }

        try {
            // 创建临时目录解压
            Path tempDir = skillPackageService.createTempDirectory();

            try {
                // 解压 zip 文件
                skillPackageService.unzip(file.getInputStream(), tempDir);

                // 查找 SKILL.md（可能在根目录或子目录中）
                Path skillMdPath = skillPackageService.findSkillMd(tempDir);
                if (skillMdPath == null) {
                    throw new SnailAiCommonException("zip 包中未找到 SKILL.md 文件");
                }

                // 解析 SKILL.md（frontmatter 校验 + 去除 frontmatter 的正文）
                String content = Files.readString(skillMdPath, StandardCharsets.UTF_8);
                SkillMarkdownParser.SkillMetadata metadata = skillMarkdownParser.extractMetadata(content);

                String name = metadata.name();
                String description = metadata.description();
                Long count = skillMapper.selectCount(new LambdaQueryWrapper<SkillPO>().eq(SkillPO::getName, name));
                if (count > 0) {
                    throw new SnailAiCommonException("{} 已经存在", name);
                }

                String skillContent = metadata.body();

                // 先创建记录获取 id
                SkillPO po = SkillPO.builder()
                        .name(name)
                        .description(description)
                        .fileName(originalFilename)
                        .fileSize(file.getSize())
                        .skillContent(skillContent)
                        .version(1L)
                        .creatorId(UserSessionUtils.currentUserSession().getId())
                        .build();
                skillMapper.insert(po);

                // 移动到持久化目录 {upload-dir}/{skillId}/（源目录为 SKILL.md 所在的目录）
                Path skillRootDir = skillMdPath.getParent();
                Path targetDir = skillPackageService.persistSkillDir(po.getId(), skillRootDir);

                // 更新 filePath 和 hasFiles
                po.setFilePath(targetDir.toAbsolutePath().toString());
                po.setHasFiles(skillFileService.hasExtraFiles(skillRootDir));
                skillMapper.updateById(po);

                // 将支撑文件保存到数据库
                if (po.getHasFiles()) {
                    skillFileService.saveSkillFilesFromZip(po.getId(), skillRootDir);
                }

                return toResponseVO(po);

            } finally {
                // 清理临时目录
                skillPackageService.deleteDirectory(tempDir);
            }

        } catch (SnailAiCommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传 Skill 包失败", e);
            throw new SnailAiCommonException("上传 Skill 包失败: ", e);
        }
    }

    /**
     * 分页查询
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 名称模糊匹配关键字，可为空
     * @return 技能分页结果
     */
    public PageResult<List<SkillResponseVO>> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SkillPO> wrapper = new LambdaQueryWrapper<SkillPO>()
                .like(StrUtil.isNotBlank(keyword), SkillPO::getName, keyword)
                .orderByDesc(SkillPO::getCreateDt);

        PageDTO<SkillPO> pageDTO = new PageDTO<>(page, size);
        IPage<SkillPO> result = skillMapper.selectPage(pageDTO, wrapper);

        List<SkillResponseVO> records = result.getRecords().stream()
                .map(this::toResponseVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageDTO, records);
    }

    /**
     * 删除 Skill
     *
     * @param id 技能 ID
     */
    @Transactional
    public void delete(Long id) {
        SkillPO po = skillMapper.selectById(id);
        if (po == null) {
            return;
        }

        // 删除关联关系
        agentSkillMapper.delete(
                new LambdaQueryWrapper<AgentSkillPO>().eq(AgentSkillPO::getSkillId, id));

        // 删除数据库中的 Skill 文件记录
        skillFileService.deleteAllBySkillId(id);

        // 删除磁盘文件
        if (StrUtil.isNotBlank(po.getFilePath())) {
            try {
                skillPackageService.deleteDirectory(Paths.get(po.getFilePath()));
            } catch (IOException e) {
                log.warn("删除 Skill 文件失败: {}", po.getFilePath(), e);
            }
        }

        skillMapper.deleteById(id);
    }

    /**
     * 全量列表（给智能体选择下拉用）
     *
     * @return 全部技能响应 VO 列表
     */
    public List<SkillResponseVO> listAll() {
        List<SkillPO> list = skillMapper.selectList(
                new LambdaQueryWrapper<SkillPO>().orderByDesc(SkillPO::getCreateDt));
        return list.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    /**
     * 按 ID 获取技能详情
     *
     * @param id 技能 ID
     * @return 技能响应 VO；不存在时抛出异常
     */
    public SkillResponseVO getById(Long id) {
        SkillPO po = skillMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + id);
        }
        return toResponseVO(po);
    }

    /**
     * 在线创建 Skill（无 zip，仅 DB）
     *
     * @param request 技能创建请求参数（名称、描述）
     * @return 创建成功的技能响应 VO
     */
    @Transactional
    public SkillResponseVO createOnline(SkillCreateRequestVO request) {
        String name = request.getName().trim();
        Long dup = skillMapper.selectCount(new LambdaQueryWrapper<SkillPO>().eq(SkillPO::getName, name));
        if (dup != null && dup > 0) {
            throw new SnailAiCommonException("技能名称已存在: " + name);
        }
        String desc = StrUtil.nullToDefault(request.getDescription(), "");
        SkillPO po = SkillPO.builder()
                .name(name)
                .description(desc)
                .fileName("online")
                .fileSize(0L)
                .skillContent("")
                .version(1L)
                .hasFiles(false)
                .creatorId(UserSessionUtils.currentUserSession().getId())
                .build();
        skillMapper.insert(po);
        return toResponseVO(po);
    }

    /**
     * 更新 Skill 元数据（名称/描述）
     *
     * @param id      技能 ID
     * @param request 技能更新请求参数（名称、描述，仅非空字段生效）
     * @return 更新后的技能响应 VO
     */
    @Transactional
    public SkillResponseVO update(Long id, SkillUpdateRequestVO request) {
        SkillPO po = skillMapper.selectById(id);
        if (po == null) {
            throw new SnailAiCommonException("技能不存在: " + id);
        }
        boolean changed = false;
        if (StrUtil.isNotBlank(request.getName())) {
            String newName = request.getName().trim();
            SkillPO other = skillMapper.selectOne(
                    new LambdaQueryWrapper<SkillPO>().eq(SkillPO::getName, newName).ne(SkillPO::getId, id));
            if (other != null) {
                throw new SnailAiCommonException("技能名称已存在: " + newName);
            }
            if (!newName.equals(po.getName())) {
                po.setName(newName);
                changed = true;
            }
        }
        if (request.getDescription() != null) {
            String desc = request.getDescription();
            if (!Objects.equals(desc, po.getDescription())) {
                po.setDescription(desc);
                changed = true;
            }
        }
        if (changed) {
            long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
            po.setVersion(newVersion);
            po.setUpdateDt(LocalDateTime.now());
            skillMapper.updateById(po);
        }
        return toResponseVO(po);
    }

    /**
     * 获取智能体关联的 Skill 列表
     *
     * @param agentId 智能体 ID
     * @return 该智能体关联的技能响应 VO 列表
     */
    public List<SkillResponseVO> getByAgentId(Long agentId) {
        List<AgentSkillPO> relations = agentSkillMapper.selectList(
                new LambdaQueryWrapper<AgentSkillPO>().eq(AgentSkillPO::getAgentId, agentId));

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> skillIds = relations.stream()
                .map(AgentSkillPO::getSkillId)
                .collect(Collectors.toList());
        List<SkillPO> skills = skillMapper.selectByIds(skillIds);
        return skills.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    /**
     * 更新智能体的 Skill 关联
     *
     * @param agentId  智能体 ID
     * @param skillIds 目标技能 ID 列表（覆盖原关联，可为空表示清空）
     */
    @Transactional
    public void updateAgentSkills(Long agentId, List<Long> skillIds) {
        // 先删除旧关联
        agentSkillMapper.delete(
                new LambdaQueryWrapper<AgentSkillPO>().eq(AgentSkillPO::getAgentId, agentId));

        // 再插入新关联
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        for (Long skillId : skillIds) {
            AgentSkillPO relation = AgentSkillPO.builder()
                    .agentId(agentId)
                    .skillId(skillId)
                    .build();
            agentSkillMapper.insert(relation);
        }
    }

    /**
     * 根据需求说明生成完整 SKILL.md 内容，不直接保存。
     */
    public SkillAiGenerateResponseVO generateDraft(SkillAiGenerateRequestVO request) {
        return skillAiContentService.generateDraft(request);
    }

    /**
     * 基于当前 SKILL.md 内容优化指定技能，不直接保存。
     */
    public SkillAiGenerateResponseVO optimizeDraft(Long skillId, SkillAiGenerateRequestVO request) {
        getById(skillId);
        String currentContent = request.getCurrentContent();
        if (StrUtil.isBlank(currentContent)) {
            currentContent = getFileContent(skillId, SKILL_MD).getContent();
        }
        return skillAiContentService.optimizeDraft(request, currentContent);
    }

    // ==================== 文件管理 API（在线编辑，委托 SkillFileService / SkillPackageService） ====================


    /**
     * 校验相对路径，禁止 .. 和绝对路径
     */
    private String normalizeRelativePath(String path) {
        if (StrUtil.isBlank(path)) {
            throw new SnailAiException("路径不能为空");
        }
        path = path.replace('\\', '/').trim();
        if (path.startsWith("/") || path.contains("..")) {
            throw new SnailAiException("非法路径: " + path);
        }
        return path;
    }

    /**
     * 构建文件树（SKILL.md + skill_file）
     *
     * @param skillId 技能 ID
     * @return 技能文件树根节点
     */
    public SkillFileTreeNodeVO buildFileTree(Long skillId) {
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + skillId);
        }
        String skillMdFull = skillMarkdownParser.rebuildSkillMd(po);
        return skillFileService.buildFileTree(skillId, skillMdFull);
    }

    /**
     * 读取文件内容（SKILL.md 为完整 Markdown；其它路径来自 skill_file）
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     * @return 文件内容 VO（内容、编码、大小）
     */
    public SkillFileContentRequestVO getFileContent(Long skillId, String path) {
        path = normalizeRelativePath(path);
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + skillId);
        }
        if (SKILL_MD.equals(path) || path.endsWith("/" + SKILL_MD)) {
            String full = skillMarkdownParser.rebuildSkillMd(po);
            byte[] bytes = full.getBytes(StandardCharsets.UTF_8);
            return SkillFileContentRequestVO.builder()
                    .content(full)
                    .encoding("utf-8")
                    .size((long) bytes.length)
                    .build();
        }
        return skillFileService.getSupportFileContent(skillId, path);
    }

    /**
     * 保存文件内容；若为 SKILL.md 则解析 frontmatter 更新 DB；写存储并失效缓存；自增版本号
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     * @param content 文件内容
     */
    @Transactional
    public void saveFileContent(Long skillId, String path, String content) {
        path = normalizeRelativePath(path);
        String body = content == null ? "" : content;

        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            return;
        }
        if (SKILL_MD.equals(path) || path.endsWith("/" + SKILL_MD)) {
            Map<String, Object> frontmatter = skillMarkdownParser.parseFrontmatter(body);
            if (frontmatter != null && !frontmatter.isEmpty()) {
                po.setName((String) frontmatter.getOrDefault("name", po.getName()));
                po.setDescription((String) frontmatter.getOrDefault("description", po.getDescription()));
            }
            po.setSkillContent(skillMarkdownParser.removeFrontmatter(body));
        } else {
            // 非 SKILL.md 文件：保存到数据库
            skillFileService.upsertContent(skillId, path, body);
        }
        // 自增版本号，重新计算文件大小
        long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
        po.setVersion(newVersion);
        po.setFileSize(skillFileService.calculateTotalFileSize(po));
        po.setUpdateDt(LocalDateTime.now());
        skillMapper.updateById(po);
    }

    /**
     * 新建文件或目录（写存储并失效树缓存；标记 hasFiles、自增版本号）
     *
     * @param skillId 技能 ID
     * @param dto     新建文件/目录请求参数（路径、类型）
     */
    @Transactional
    public void createFile(Long skillId, SkillCreateFileRequestVO dto) {
        dto.setPath(normalizeRelativePath(dto.getPath()));

        if ("file".equals(dto.getType())) {
            skillFileService.createEmptyFile(skillId, dto.getPath());
        } else if ("directory".equals(dto.getType())) {
            skillFileService.createDirectoryMarker(skillId, dto.getPath());
        }

        // 新建了支撑文件，标记 hasFiles = true 并自增版本号
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            return;
        }
        if (!Boolean.TRUE.equals(po.getHasFiles())) {
            po.setHasFiles(true);
        }
        long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
        po.setVersion(newVersion);
        po.setFileSize(skillFileService.calculateTotalFileSize(po));
        po.setUpdateDt(LocalDateTime.now());
        skillMapper.updateById(po);
    }

    /**
     * 删除文件；禁止删除 SKILL.md（写存储并失效缓存；重新判断 hasFiles、自增版本号）
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     */
    @Transactional
    public void deleteFile(Long skillId, String path) {
        path = normalizeRelativePath(path);
        if (SKILL_MD.equals(path) || path.endsWith("/" + SKILL_MD)) {
            throw new SnailAiException("不能删除 SKILL.md");
        }

        // 从数据库中删除文件
        skillFileService.deleteBySkillIdAndPath(skillId, path);

        // 删除后重新判断是否还有支撑文件，并自增版本号
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            return;
        }
        // 检查是否还有其他文件
        long fileCount = skillFileService.countBySkillId(skillId);
        po.setHasFiles(fileCount > 0);
        long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
        po.setVersion(newVersion);
        po.setFileSize(skillFileService.calculateTotalFileSize(po));
        po.setUpdateDt(LocalDateTime.now());
        skillMapper.updateById(po);
    }

    /**
     * 重命名文件或目录（写存储并失效缓存；自增版本号）
     *
     * @param skillId 技能 ID
     * @param dto     重命名请求参数（旧路径、新路径）
     */
    @Transactional
    public void renameFile(Long skillId, SkillRenameFileRequestVO dto) {
        dto.setOldPath(normalizeRelativePath(dto.getOldPath()));
        dto.setNewPath(normalizeRelativePath(dto.getNewPath()));

        skillFileService.rename(skillId, dto.getOldPath(), dto.getNewPath());

        // 自增版本号
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            return;
        }
        long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
        po.setVersion(newVersion);
        po.setFileSize(skillFileService.calculateTotalFileSize(po));
        po.setUpdateDt(LocalDateTime.now());
        skillMapper.updateById(po);
    }

    /**
     * 将技能目录打包为 zip 写入输出流（用于下载；基于 DB 内容）
     *
     * @param skillId 技能 ID
     * @param out     目标输出流（写入 zip 字节）
     */
    public void writeZipToStream(Long skillId, OutputStream out) {
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + skillId);
        }
        skillPackageService.writeZipToStream(po, out);
    }

    private SkillResponseVO toResponseVO(SkillPO po) {
        return SkillResponseVO.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .fileName(po.getFileName())
                .fileSize(po.getFileSize())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }
}
