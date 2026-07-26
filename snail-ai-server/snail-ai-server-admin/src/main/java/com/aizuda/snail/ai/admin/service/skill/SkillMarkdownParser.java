package com.aizuda.snail.ai.admin.service.skill;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.persistence.skill.po.SkillPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * SKILL.md 解析器，集中承担 YAML frontmatter 解析、正文提取、元数据（name/description）校验
 * 与 SKILL.md 全文重建等纯解析逻辑，不涉及任何持久化与文件系统操作。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
public class SkillMarkdownParser {

    /**
     * SKILL.md 元数据解析结果
     *
     * @param name        frontmatter 中的技能名称
     * @param description frontmatter 中的技能描述
     * @param body        去除 frontmatter 后的正文内容
     */
    public record SkillMetadata(String name, String description, String body) {
    }

    /**
     * 解析并校验 SKILL.md 元数据：要求存在 frontmatter 且 name、description 均非空，
     * 校验通过后返回元数据与去除 frontmatter 的正文
     *
     * @param content SKILL.md 完整内容
     * @return 元数据解析结果；校验不通过时抛出异常
     */
    public SkillMetadata extractMetadata(String content) {
        Map<String, Object> frontmatter = parseFrontmatter(content);
        if (frontmatter == null || frontmatter.isEmpty()) {
            throw new SnailAiCommonException("SKILL.md 缺少 YAML frontmatter (---...---)");
        }
        String name = (String) frontmatter.get("name");
        String description = (String) frontmatter.get("description");
        if (StrUtil.isBlank(name)) {
            log.warn("SKILL.md frontmatter 校验失败：缺少 name 字段");
            throw new SnailAiCommonException("SKILL.md frontmatter 中缺少 name 字段");
        }
        if (StrUtil.isBlank(description)) {
            log.warn("SKILL.md frontmatter 校验失败：缺少 description 字段");
            throw new SnailAiCommonException("SKILL.md frontmatter 中缺少 description 字段");
        }
        return new SkillMetadata(name, description, removeFrontmatter(content));
    }

    /**
     * 解析 YAML frontmatter
     *
     * @param content Markdown 全文
     * @return frontmatter 键值对；无 frontmatter 或解析失败时返回 null
     */
    public Map<String, Object> parseFrontmatter(String content) {
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
     *
     * @param content Markdown 全文
     * @return 去除 frontmatter 后的正文；无 frontmatter 时原样返回
     */
    public String removeFrontmatter(String content) {
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
     * 根据 SkillPO 重建 SKILL.md（frontmatter + 正文）
     *
     * @param po 技能持久化对象
     * @return SKILL.md 完整内容
     */
    public String rebuildSkillMd(SkillPO po) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(StrUtil.nullToDefault(po.getName(), "")).append("\n");
        sb.append("description: \"").append(StrUtil.nullToDefault(po.getDescription(), "")).append("\"\n");
        sb.append("---\n");
        sb.append(StrUtil.nullToDefault(po.getSkillContent(), ""));
        return sb.toString();
    }
}
