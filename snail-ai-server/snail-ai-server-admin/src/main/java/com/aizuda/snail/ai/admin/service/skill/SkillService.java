package com.aizuda.snail.ai.admin.service.skill;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentSkillMapper;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillFileMapper;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillMapper;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.skill.SkillCreateFileRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillRenameFileRequestVO;
import com.aizuda.snail.ai.persistence.agent.po.AgentSkillPO;
import com.aizuda.snail.ai.persistence.skill.po.SkillFilePO;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.aizuda.snail.ai.admin.vo.skill.SkillCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileContentRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileTreeNodeVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillResponseVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillUpdateRequestVO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.aizuda.snail.ai.common.constants.SystemConstants.SKILL_MD;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillMapper skillMapper;
    private final SkillFileMapper skillFileMapper;
    private final AgentSkillMapper agentSkillMapper;
    @Value("${snail-ai.skill.upload-dir:./upload/skills}")
    private String skillUploadDir;

    /**
     * 上传 Skill zip 包
     */
    public SkillResponseVO upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new SnailAiException("仅支持上传 .zip 格式的 Skill 包");
        }

        try {
            // 创建临时目录解压
            Path tempDir = Files.createTempDirectory("skill-upload-");

            try {
                // 解压 zip 文件
                unzip(file.getInputStream(), tempDir);

                // 查找 SKILL.md（可能在根目录或子目录中）
                Path skillMdPath = findSkillMd(tempDir);
                if (skillMdPath == null) {
                    throw new SnailAiCommonException("zip 包中未找到 SKILL.md 文件");
                }

                // 解析 SKILL.md
                String content = Files.readString(skillMdPath, StandardCharsets.UTF_8);
                Map<String, Object> frontmatter = parseFrontmatter(content);
                if (frontmatter == null || frontmatter.isEmpty()) {
                    throw new SnailAiCommonException("SKILL.md 缺少 YAML frontmatter (---...---)");
                }

                String name = (String) frontmatter.get("name");
                String description = (String) frontmatter.get("description");
                if (StrUtil.isBlank(name)) {
                    throw new SnailAiCommonException("SKILL.md frontmatter 中缺少 name 字段");
                }
                if (StrUtil.isBlank(description)) {
                    throw new SnailAiCommonException("SKILL.md frontmatter 中缺少 description 字段");
                }
                Long count = skillMapper.selectCount(new LambdaQueryWrapper<SkillPO>().eq(SkillPO::getName, name));
                if (count > 0) {
                    throw new SnailAiCommonException("{} 已经存在", name);
                }

                // 去除 frontmatter，保留正文
                String skillContent = removeFrontmatter(content);

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

                // 移动到持久化目录 {upload-dir}/{skillId}/
                Path targetDir = Paths.get(skillUploadDir, String.valueOf(po.getId()));
                Files.createDirectories(targetDir);

                // 确定要移动的源目录（SKILL.md 所在的目录）
                Path skillRootDir = skillMdPath.getParent();
                copyDirectory(skillRootDir, targetDir);

                // 更新 filePath 和 hasFiles
                po.setFilePath(targetDir.toAbsolutePath().toString());
                po.setHasFiles(hasExtraFiles(skillRootDir));
                skillMapper.updateById(po);

                // 将支撑文件保存到数据库
                if (po.getHasFiles()) {
                    saveSkillFilesFromZip(po.getId(), skillRootDir);
                }

                return toResponseVO(po);

            } finally {
                // 清理临时目录
                deleteDirectory(tempDir);
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
        skillFileMapper.delete(new LambdaQueryWrapper<SkillFilePO>().eq(SkillFilePO::getSkillId, id));

        // 删除磁盘文件
        if (StrUtil.isNotBlank(po.getFilePath())) {
            try {
                deleteDirectory(Paths.get(po.getFilePath()));
            } catch (IOException e) {
                log.warn("删除 Skill 文件失败: {}", po.getFilePath(), e);
            }
        }

        skillMapper.deleteById(id);
    }

    /**
     * 全量列表（给智能体选择下拉用）
     */
    public List<SkillResponseVO> listAll() {
        List<SkillPO> list = skillMapper.selectList(
                new LambdaQueryWrapper<SkillPO>().orderByDesc(SkillPO::getCreateDt));
        return list.stream().map(this::toResponseVO).collect(Collectors.toList());
    }

    public SkillResponseVO getById(Long id) {
        SkillPO po = skillMapper.selectById(id);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + id);
        }
        return toResponseVO(po);
    }

    /**
     * 在线创建 Skill（无 zip，仅 DB）
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
     */
    @Transactional
    public void updateAgentSkills(Long agentId, List<Long> skillIds) {
        // 先删除旧关联
        agentSkillMapper.delete(
                new LambdaQueryWrapper<AgentSkillPO>().eq(AgentSkillPO::getAgentId, agentId));

        // 再插入新关联
        if (skillIds != null && !skillIds.isEmpty()) {
            for (Long skillId : skillIds) {
                AgentSkillPO relation = AgentSkillPO.builder()
                        .agentId(agentId)
                        .skillId(skillId)
                        .build();
                agentSkillMapper.insert(relation);
            }
        }
    }


    // ==================== 文件管理 API（在线编辑，经 SkillCacheService + SkillStorageService） ====================


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
     */
    public SkillFileTreeNodeVO buildFileTree(Long skillId) {
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + skillId);
        }
        String skillMdFull = rebuildSkillMd(po);
        long skillMdSize = skillMdFull.getBytes(StandardCharsets.UTF_8).length;
        SkillFileTreeNodeVO skillMdNode = SkillFileTreeNodeVO.builder()
                .name(SKILL_MD)
                .type("file")
                .size(skillMdSize)
                .build();

        List<SkillFileTreeNodeVO> children = new ArrayList<>();
        children.add(skillMdNode);

        List<SkillFilePO> files = skillFileMapper.selectList(
                new LambdaQueryWrapper<SkillFilePO>()
                        .eq(SkillFilePO::getSkillId, skillId)
                        .orderByAsc(SkillFilePO::getFilePath));
        for (SkillFilePO f : files) {
            String fp = f.getFilePath();
            if (fp == null) {
                continue;
            }
            String norm = fp.replace('\\', '/').trim();
            if (SKILL_MD.equalsIgnoreCase(norm) || norm.endsWith("/" + SKILL_MD)) {
                continue;
            }
            long sz = f.getFileSize() != null ? f.getFileSize().longValue() : 0L;
            insertPathIntoTree(children, norm, sz);
        }
        sortTreeChildren(children);
        return SkillFileTreeNodeVO.builder()
                .name("/")
                .type("directory")
                .children(children)
                .build();
    }

    private void insertPathIntoTree(List<SkillFileTreeNodeVO> rootChildren, String relativePath, long size) {
        String p = relativePath.replace('\\', '/').trim();
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (StrUtil.isBlank(p)) {
            return;
        }
        String[] parts = p.split("/");
        List<SkillFileTreeNodeVO> level = rootChildren;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (StrUtil.isBlank(part)) {
                continue;
            }
            boolean isLast = i == parts.length - 1;
            if (isLast) {
                level.add(SkillFileTreeNodeVO.builder()
                        .name(part)
                        .type("file")
                        .size(size)
                        .build());
            } else {
                SkillFileTreeNodeVO dir = findOrCreateDir(level, part);
                if (dir.getChildren() == null) {
                    dir.setChildren(new ArrayList<>());
                }
                level = dir.getChildren();
            }
        }
    }

    private SkillFileTreeNodeVO findOrCreateDir(List<SkillFileTreeNodeVO> siblings, String name) {
        for (SkillFileTreeNodeVO n : siblings) {
            if ("directory".equals(n.getType()) && name.equals(n.getName())) {
                return n;
            }
        }
        SkillFileTreeNodeVO dir = SkillFileTreeNodeVO.builder()
                .name(name)
                .type("directory")
                .children(new ArrayList<>())
                .build();
        siblings.add(dir);
        return dir;
    }

    private void sortTreeChildren(List<SkillFileTreeNodeVO> children) {
        if (children == null) {
            return;
        }
        children.sort((a, b) -> {
            boolean skillA = SKILL_MD.equalsIgnoreCase(a.getName());
            boolean skillB = SKILL_MD.equalsIgnoreCase(b.getName());
            if (skillA != skillB) {
                return skillA ? -1 : 1;
            }
            boolean da = "directory".equals(a.getType());
            boolean db = "directory".equals(b.getType());
            if (da != db) {
                return da ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (SkillFileTreeNodeVO c : children) {
            if (c.getChildren() != null) {
                sortTreeChildren(c.getChildren());
            }
        }
    }

    /**
     * 读取文件内容（SKILL.md 为完整 Markdown；其它路径来自 skill_file）
     */
    public SkillFileContentRequestVO getFileContent(Long skillId, String path) {
        path = normalizeRelativePath(path);
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + skillId);
        }
        if (SKILL_MD.equals(path) || path.endsWith("/" + SKILL_MD)) {
            String full = rebuildSkillMd(po);
            byte[] bytes = full.getBytes(StandardCharsets.UTF_8);
            return SkillFileContentRequestVO.builder()
                    .content(full)
                    .encoding("utf-8")
                    .size((long) bytes.length)
                    .build();
        }
        SkillFilePO f = skillFileMapper.selectOne(new LambdaQueryWrapper<SkillFilePO>()
                .eq(SkillFilePO::getSkillId, skillId)
                .eq(SkillFilePO::getFilePath, path));
        if (f == null) {
            throw new SnailAiException("文件不存在: " + path);
        }
        String c = f.getContent() != null ? f.getContent() : "";
        byte[] bytes = c.getBytes(StandardCharsets.UTF_8);
        return SkillFileContentRequestVO.builder()
                .content(c)
                .encoding("utf-8")
                .size((long) bytes.length)
                .build();
    }

    /**
     * 保存文件内容；若为 SKILL.md 则解析 frontmatter 更新 DB；写存储并失效缓存；自增版本号
     */
    @Transactional
    public void saveFileContent(Long skillId, String path, String content) {
        path = normalizeRelativePath(path);
        String body = content == null ? "" : content;

        SkillPO po = skillMapper.selectById(skillId);
        if (po != null) {
            if (SKILL_MD.equals(path) || path.endsWith("/" + SKILL_MD)) {
                Map<String, Object> frontmatter = parseFrontmatter(body);
                if (frontmatter != null && !frontmatter.isEmpty()) {
                    po.setName((String) frontmatter.getOrDefault("name", po.getName()));
                    po.setDescription((String) frontmatter.getOrDefault("description", po.getDescription()));
                }
                po.setSkillContent(removeFrontmatter(body));
            } else {
                // 非 SKILL.md 文件：保存到数据库
                int fileSize = body.length();
                SkillFilePO existingFile = skillFileMapper.selectOne(new LambdaQueryWrapper<SkillFilePO>()
                        .eq(SkillFilePO::getSkillId, skillId)
                        .eq(SkillFilePO::getFilePath, path));
                if (existingFile != null) {
                    existingFile.setContent(body);
                    existingFile.setFileSize(fileSize);
                    existingFile.setUpdatedAt(LocalDateTime.now());
                    skillFileMapper.updateById(existingFile);
                } else {
                    SkillFilePO newFile = SkillFilePO.builder()
                            .skillId(skillId)
                            .filePath(path)
                            .content(body)
                            .fileSize(fileSize)
                            .encoding("utf-8")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    skillFileMapper.insert(newFile);
                }
            }
            // 自增版本号，重新计算文件大小
            long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
            po.setVersion(newVersion);
            po.setFileSize(calculateTotalFileSize(po));
            po.setUpdateDt(LocalDateTime.now());
            skillMapper.updateById(po);
        }
    }

    /**
     * 新建文件或目录（写存储并失效树缓存；标记 hasFiles、自增版本号）
     */
    @Transactional
    public void createFile(Long skillId, SkillCreateFileRequestVO dto) {
        dto.setPath(normalizeRelativePath(dto.getPath()));

        if ("file".equals(dto.getType())) {
            SkillFilePO newFile = SkillFilePO.builder()
                    .skillId(skillId)
                    .filePath(dto.getPath())
                    .content("")
                    .fileSize(0)
                    .encoding("utf-8")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            skillFileMapper.insert(newFile);
        } else if ("directory".equals(dto.getType())) {
            String base = dto.getPath().replaceAll("/+$", "");
            if (StrUtil.isBlank(base)) {
                throw new SnailAiException("目录路径不能为空");
            }
            String markerPath = base + "/.keep";
            Long exists = skillFileMapper.selectCount(new LambdaQueryWrapper<SkillFilePO>()
                    .eq(SkillFilePO::getSkillId, skillId)
                    .eq(SkillFilePO::getFilePath, markerPath));
            if (exists != null && exists > 0) {
                throw new SnailAiException("目录已存在: " + base);
            }
            SkillFilePO marker = SkillFilePO.builder()
                    .skillId(skillId)
                    .filePath(markerPath)
                    .content("")
                    .fileSize(0)
                    .encoding("utf-8")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            skillFileMapper.insert(marker);
        }

        // 新建了支撑文件，标记 hasFiles = true 并自增版本号
        SkillPO po = skillMapper.selectById(skillId);
        if (po != null) {
            if (!Boolean.TRUE.equals(po.getHasFiles())) {
                po.setHasFiles(true);
            }
            long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
            po.setVersion(newVersion);
            po.setFileSize(calculateTotalFileSize(po));
            po.setUpdateDt(LocalDateTime.now());
            skillMapper.updateById(po);
        }
    }

    /**
     * 删除文件；禁止删除 SKILL.md（写存储并失效缓存；重新判断 hasFiles、自增版本号）
     */
    @Transactional
    public void deleteFile(Long skillId, String path) {
        path = normalizeRelativePath(path);
        if (SKILL_MD.equals(path) || path.endsWith("/" + SKILL_MD)) {
            throw new SnailAiException("不能删除 SKILL.md");
        }

        // 从数据库中删除文件
        skillFileMapper.delete(new LambdaQueryWrapper<SkillFilePO>()
                .eq(SkillFilePO::getSkillId, skillId)
                .eq(SkillFilePO::getFilePath, path));

        // 删除后重新判断是否还有支撑文件，并自增版本号
        SkillPO po = skillMapper.selectById(skillId);
        if (po != null) {
            // 检查是否还有其他文件
            long fileCount = skillFileMapper.selectCount(new LambdaQueryWrapper<SkillFilePO>()
                    .eq(SkillFilePO::getSkillId, skillId));
            po.setHasFiles(fileCount > 0);
            long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
            po.setVersion(newVersion);
            po.setFileSize(calculateTotalFileSize(po));
            po.setUpdateDt(LocalDateTime.now());
            skillMapper.updateById(po);
        }
    }

    /**
     * 重命名文件或目录（写存储并失效缓存；自增版本号）
     */
    @Transactional
    public void renameFile(Long skillId, SkillRenameFileRequestVO dto) {
        dto.setOldPath(normalizeRelativePath(dto.getOldPath()));
        dto.setNewPath(normalizeRelativePath(dto.getNewPath()));

        // 从数据库中读取旧文件
        SkillFilePO oldFile = skillFileMapper.selectOne(new LambdaQueryWrapper<SkillFilePO>()
                .eq(SkillFilePO::getSkillId, skillId)
                .eq(SkillFilePO::getFilePath, dto.getOldPath()));
        if (oldFile != null) {
            // 创建新文件记录
            SkillFilePO newFile = SkillFilePO.builder()
                    .skillId(skillId)
                    .filePath(dto.getNewPath())
                    .content(oldFile.getContent())
                    .fileSize(oldFile.getFileSize())
                    .encoding(oldFile.getEncoding())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            skillFileMapper.insert(newFile);
            // 删除旧文件记录
            skillFileMapper.deleteById(oldFile.getId());
        }

        // 自增版本号
        SkillPO po = skillMapper.selectById(skillId);
        if (po != null) {
            long newVersion = (po.getVersion() != null ? po.getVersion() : 0L) + 1;
            po.setVersion(newVersion);
            po.setFileSize(calculateTotalFileSize(po));
            po.setUpdateDt(LocalDateTime.now());
            skillMapper.updateById(po);
        }
    }

    /**
     * 将技能目录打包为 zip 写入输出流（用于下载；基于 DB 内容）
     */
    public void writeZipToStream(Long skillId, OutputStream out) {
        SkillPO po = skillMapper.selectById(skillId);
        if (po == null) {
            throw new SnailAiException("技能不存在: " + skillId);
        }
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            String skillMd = rebuildSkillMd(po);
            ZipEntry skillEntry = new ZipEntry(SKILL_MD);
            zos.putNextEntry(skillEntry);
            zos.write(skillMd.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            List<SkillFilePO> files = skillFileMapper.selectList(
                    new LambdaQueryWrapper<SkillFilePO>().eq(SkillFilePO::getSkillId, skillId));
            for (SkillFilePO f : files) {
                String entryName = normalizeZipEntryName(f.getFilePath());
                ZipEntry fe = new ZipEntry(entryName);
                zos.putNextEntry(fe);
                String body = f.getContent() != null ? f.getContent() : "";
                zos.write(body.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException e) {
            log.error("打包 Skill zip 失败", e);
            throw new SnailAiException("打包 Skill zip 失败: " + e.getMessage());
        }
    }

    private String normalizeZipEntryName(String path) {
        if (StrUtil.isBlank(path)) {
            throw new SnailAiException("非法 zip 条目路径");
        }
        String p = path.replace('\\', '/').trim();
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.contains("..")) {
            throw new SnailAiException("非法 zip 条目路径: " + path);
        }
        return p;
    }

    /**
     * 上传 Skill 时，将支撑文件内容保存到数据库
     */
    private void saveSkillFilesFromZip(Long skillId, Path unzipPath) {
        try (var stream = Files.walk(unzipPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().equals(SKILL_MD))
                    .forEach(filePath -> {
                        try {
                            String relativePath = unzipPath.relativize(filePath).toString()
                                    .replace('\\', '/');
                            String content = Files.readString(filePath, StandardCharsets.UTF_8);
                            int fileSize = (int) Files.size(filePath);

                            SkillFilePO fileContent = SkillFilePO.builder()
                                    .skillId(skillId)
                                    .filePath(relativePath)
                                    .content(content)
                                    .fileSize(fileSize)
                                    .encoding("utf-8")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build();
                            skillFileMapper.insert(fileContent);
                        } catch (IOException e) {
                            log.error("保存文件到数据库失败: {}", filePath, e);
                        }
                    });

        } catch (IOException e) {
            log.error("扫描 Skill 文件失败: {}", unzipPath, e);
        }
    }


    /**
     * 根据 SkillPO 重建 SKILL.md（frontmatter + 正文）
     */
    private String rebuildSkillMd(SkillPO po) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(StrUtil.nullToDefault(po.getName(), "")).append("\n");
        sb.append("description: \"").append(StrUtil.nullToDefault(po.getDescription(), "")).append("\"\n");
        sb.append("---\n");
        sb.append(StrUtil.nullToDefault(po.getSkillContent(), ""));
        return sb.toString();
    }

    /**
     * 重新计算 Skill 的总文件大小（SKILL.md + 所有支撑文件）
     */
    private long calculateTotalFileSize(SkillPO po) {
        long skillMdSize = rebuildSkillMd(po).getBytes(StandardCharsets.UTF_8).length;
        List<SkillFilePO> files = skillFileMapper.selectList(
                new LambdaQueryWrapper<SkillFilePO>().eq(SkillFilePO::getSkillId, po.getId()));
        long supportFilesSize = files.stream()
                .mapToLong(f -> f.getContent() != null ? f.getContent().getBytes(StandardCharsets.UTF_8).length : 0L)
                .sum();
        return skillMdSize + supportFilesSize;
    }

    // ==================== 私有方法 ====================

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

    /**
     * 解压 zip 文件，防止 zip slip 攻击
     * 使用 Apache Commons Compress 提高 ZIP 兼容性，支持 data descriptor
     */
    private void unzip(InputStream inputStream, Path targetDir) throws IOException {
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(inputStream, StandardCharsets.UTF_8.name(), false, true)) {
            ArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                // 防止 zip slip
                if (!entryPath.startsWith(targetDir)) {
                    throw new SnailAiException("非法 zip 条目路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * 在目录中查找 SKILL.md 文件
     */
    private Path findSkillMd(Path dir) throws IOException {
        // 先检查根目录
        Path root = dir.resolve("SKILL.md");
        if (Files.exists(root)) {
            return root;
        }
        // 检查一级子目录（zip 内可能有一层包装目录）
        try (var stream = Files.list(dir)) {
            Optional<Path> found = stream
                    .filter(Files::isDirectory)
                    .map(d -> d.resolve("SKILL.md"))
                    .filter(Files::exists)
                    .findFirst();
            return found.orElse(null);
        }
    }

    /**
     * 解析 YAML frontmatter
     */
    private Map<String, Object> parseFrontmatter(String content) {
        if (!content.startsWith("---")) {
            return null;
        }
        int endIndex = content.indexOf("---", 3);
        if (endIndex == -1) {
            return null;
        }
        String frontmatterStr = content.substring(3, endIndex).trim();
        try {
            Yaml yaml = new Yaml();
            return yaml.load(frontmatterStr);
        } catch (Exception e) {
            log.error("解析 YAML frontmatter 失败", e);
            return null;
        }
    }

    /**
     * 去除 frontmatter，返回正文内容
     */
    private String removeFrontmatter(String content) {
        if (!content.startsWith("---")) {
            return content;
        }
        int endIndex = content.indexOf("---", 3);
        if (endIndex == -1) {
            return content;
        }
        return content.substring(endIndex + 3).trim();
    }

    /**
     * 复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            stream.forEach(src -> {
                Path dest = target.resolve(source.relativize(src));
                try {
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
        }
    }

    /**
     * 判断解压后的技能目录是否包含 SKILL.md 以外的其他文件
     */
    private boolean hasExtraFiles(Path skillRootDir) {
        try (var stream = Files.walk(skillRootDir)) {
            return stream.filter(Files::isRegularFile)
                    .anyMatch(p -> !p.getFileName().toString().equals(SKILL_MD));
        } catch (IOException e) {
            log.warn("检查支撑文件失败: {}", skillRootDir, e);
            return false;
        }
    }


}
