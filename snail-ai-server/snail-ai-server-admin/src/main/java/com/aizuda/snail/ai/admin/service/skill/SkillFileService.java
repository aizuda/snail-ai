package com.aizuda.snail.ai.admin.service.skill;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileContentRequestVO;
import com.aizuda.snail.ai.admin.vo.skill.SkillFileTreeNodeVO;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.persistence.skill.mapper.SkillFileMapper;
import com.aizuda.snail.ai.persistence.skill.po.SkillFilePO;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.aizuda.snail.ai.common.constants.SystemConstants.SKILL_MD;

/**
 * 技能支撑文件服务，集中承担 skill_file 表的读取与落库：文件内容查询、在线编辑的新增/修改/删除/重命名、
 * 上传 zip 后支撑文件批量入库、文件树构建等文件相关持久化逻辑。事务由门面 {@link SkillService} 的
 * public 方法统一开启，本类不声明事务注解。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillFileService {

    private final SkillFileMapper skillFileMapper;
    private final SkillMarkdownParser skillMarkdownParser;

    /**
     * 查询技能的全部支撑文件记录（不排序，用于打包导出与总大小计算）
     *
     * @param skillId 技能 ID
     * @return 支撑文件记录列表
     */
    public List<SkillFilePO> listBySkillId(Long skillId) {
        return skillFileMapper.selectList(
                new LambdaQueryWrapper<SkillFilePO>().eq(SkillFilePO::getSkillId, skillId));
    }

    /**
     * 重新计算 Skill 的总文件大小（SKILL.md + 所有支撑文件）
     *
     * @param po 技能持久化对象
     * @return 总字节数
     */
    public long calculateTotalFileSize(SkillPO po) {
        long skillMdSize = skillMarkdownParser.rebuildSkillMd(po).getBytes(StandardCharsets.UTF_8).length;
        List<SkillFilePO> files = listBySkillId(po.getId());
        long supportFilesSize = files.stream()
                .mapToLong(f -> f.getContent() != null ? f.getContent().getBytes(StandardCharsets.UTF_8).length : 0L)
                .sum();
        return skillMdSize + supportFilesSize;
    }

    /**
     * 构建文件树（SKILL.md + skill_file）
     *
     * @param skillId     技能 ID
     * @param skillMdFull 重建后的 SKILL.md 完整内容（用于计算大小）
     * @return 技能文件树根节点
     */
    public SkillFileTreeNodeVO buildFileTree(Long skillId, String skillMdFull) {
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
     * 读取支撑文件内容（来自 skill_file 表）
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     * @return 文件内容 VO（内容、编码、大小）；文件不存在时抛出异常
     */
    public SkillFileContentRequestVO getSupportFileContent(Long skillId, String path) {
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
     * 保存支撑文件内容：已存在则更新，不存在则新增
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     * @param body    文件内容（非空字符串）
     */
    public void upsertContent(Long skillId, String path, String body) {
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

    /**
     * 新建空文件记录
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     */
    public void createEmptyFile(Long skillId, String path) {
        SkillFilePO newFile = SkillFilePO.builder()
                .skillId(skillId)
                .filePath(path)
                .content("")
                .fileSize(0)
                .encoding("utf-8")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        skillFileMapper.insert(newFile);
    }

    /**
     * 新建目录（以 .keep 占位文件标记）；目录已存在时抛出异常
     *
     * @param skillId 技能 ID
     * @param path    目录相对路径
     */
    public void createDirectoryMarker(Long skillId, String path) {
        String base = path.replaceAll("/+$", "");
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

    /**
     * 删除指定路径的支撑文件记录
     *
     * @param skillId 技能 ID
     * @param path    技能内相对路径
     */
    public void deleteBySkillIdAndPath(Long skillId, String path) {
        skillFileMapper.delete(new LambdaQueryWrapper<SkillFilePO>()
                .eq(SkillFilePO::getSkillId, skillId)
                .eq(SkillFilePO::getFilePath, path));
    }

    /**
     * 删除技能的全部支撑文件记录
     *
     * @param skillId 技能 ID
     */
    public void deleteAllBySkillId(Long skillId) {
        skillFileMapper.delete(new LambdaQueryWrapper<SkillFilePO>().eq(SkillFilePO::getSkillId, skillId));
    }

    /**
     * 统计技能的支撑文件数量
     *
     * @param skillId 技能 ID
     * @return 支撑文件条数
     */
    public Long countBySkillId(Long skillId) {
        return skillFileMapper.selectCount(new LambdaQueryWrapper<SkillFilePO>()
                .eq(SkillFilePO::getSkillId, skillId));
    }

    /**
     * 重命名文件：以新路径复制记录后删除旧记录；旧文件不存在时跳过
     *
     * @param skillId 技能 ID
     * @param oldPath 旧相对路径
     * @param newPath 新相对路径
     */
    public void rename(Long skillId, String oldPath, String newPath) {
        // 从数据库中读取旧文件
        SkillFilePO oldFile = skillFileMapper.selectOne(new LambdaQueryWrapper<SkillFilePO>()
                .eq(SkillFilePO::getSkillId, skillId)
                .eq(SkillFilePO::getFilePath, oldPath));
        if (oldFile == null) {
            log.warn("重命名源文件不存在，跳过: skillId={}, path={}", skillId, oldPath);
            return;
        }
        // 创建新文件记录
        SkillFilePO newFile = SkillFilePO.builder()
                .skillId(skillId)
                .filePath(newPath)
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

    /**
     * 上传 Skill 时，将支撑文件内容保存到数据库
     *
     * @param skillId   技能 ID
     * @param unzipPath 解压后的技能根目录
     */
    public void saveSkillFilesFromZip(Long skillId, Path unzipPath) {
        try (var stream = Files.walk(unzipPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !SKILL_MD.equals(p.getFileName().toString()))
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
     * 判断解压后的技能目录是否包含 SKILL.md 以外的其他文件
     *
     * @param skillRootDir 解压后的技能根目录
     * @return 是否存在支撑文件
     */
    public boolean hasExtraFiles(Path skillRootDir) {
        try (var stream = Files.walk(skillRootDir)) {
            return stream.filter(Files::isRegularFile)
                    .anyMatch(p -> !SKILL_MD.equals(p.getFileName().toString()));
        } catch (IOException e) {
            log.warn("检查支撑文件失败: {}", skillRootDir, e);
            return false;
        }
    }
}
