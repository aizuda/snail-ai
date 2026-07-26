package com.aizuda.snail.ai.admin.service.skill;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.persistence.skill.po.SkillFilePO;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.aizuda.snail.ai.common.constants.SystemConstants.SKILL_MD;

/**
 * 技能包服务，集中承担技能包的文件系统与压缩包处理：zip 解压（含 Zip Slip 防护）、SKILL.md 定位、
 * 临时目录创建与清理、技能目录持久化落盘，以及基于数据库内容将技能打包为 zip 导出。
 * 不承担任何数据库写操作，事务由门面 {@link SkillService} 统一管理。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillPackageService {

    private final SkillFileService skillFileService;
    private final SkillMarkdownParser skillMarkdownParser;
    @Value("${snail-ai.skill.upload-dir:./upload/skills}")
    private String skillUploadDir;

    /**
     * 创建技能上传解压用的临时目录
     *
     * @return 临时目录路径
     */
    public Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("skill-upload-");
    }

    /**
     * 解压 zip 文件，防止 zip slip 攻击
     * 使用 Apache Commons Compress 提高 ZIP 兼容性，支持 data descriptor
     *
     * @param inputStream zip 输入流
     * @param targetDir   解压目标目录
     */
    public void unzip(InputStream inputStream, Path targetDir) throws IOException {
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
     *
     * @param dir 解压后的根目录
     * @return SKILL.md 路径；根目录及一级子目录均未找到时返回 null
     */
    public Path findSkillMd(Path dir) throws IOException {
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
     * 将解压后的技能目录复制到持久化目录 {upload-dir}/{skillId}/
     *
     * @param skillId      技能 ID
     * @param skillRootDir SKILL.md 所在的技能根目录
     * @return 持久化目录路径
     */
    public Path persistSkillDir(Long skillId, Path skillRootDir) throws IOException {
        Path targetDir = Paths.get(skillUploadDir, String.valueOf(skillId));
        Files.createDirectories(targetDir);
        copyDirectory(skillRootDir, targetDir);
        return targetDir;
    }

    /**
     * 删除目录及其内容
     *
     * @param dir 目标目录（不存在时直接返回）
     */
    public void deleteDirectory(Path dir) throws IOException {
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
     * 将技能打包为 zip 写入输出流（用于下载；基于 DB 内容：重建的 SKILL.md + 支撑文件）
     *
     * @param po  技能持久化对象
     * @param out 目标输出流（写入 zip 字节）
     */
    public void writeZipToStream(SkillPO po, OutputStream out) {
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            String skillMd = skillMarkdownParser.rebuildSkillMd(po);
            ZipEntry skillEntry = new ZipEntry(SKILL_MD);
            zos.putNextEntry(skillEntry);
            zos.write(skillMd.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            List<SkillFilePO> files = skillFileService.listBySkillId(po.getId());
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

    /**
     * 规范化 zip 条目路径：统一分隔符、去除前导斜杠，并拒绝路径穿越
     */
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
}
