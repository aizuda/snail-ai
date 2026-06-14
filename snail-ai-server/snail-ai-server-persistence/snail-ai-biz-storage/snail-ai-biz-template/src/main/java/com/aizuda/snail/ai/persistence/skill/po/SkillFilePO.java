package com.aizuda.snail.ai.persistence.skill.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 技能支撑文件持久化对象
 * 表: sai_skill_file
 *
 * 存储Skill包中的支撑文件（脚本、文档、参考资料等）
 * 这些文件与SKILL.md定义一起组成完整的技能包
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_skill_file")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkillFilePO {

    /**
     * 文件ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 技能ID (外键)
     * 关联到 sai_skill.id
     * 该文件属于的技能包
     */
    private Long skillId;

    /**
     * 文件相对路径
     * 相对于技能根目录的相对路径
     * 示例: scripts/run.py, references/api.md, examples/usage.txt
     * 用于恢复目录结构和文件组织
     */
    private String filePath;

    /**
     * 文件内容
     * 支撑文件的完整内容
     * 根据encoding字段决定是否进行base64编码
     * 可为null（指向外部存储）
     */
    private String content;

    /**
     * 文件大小 (字节)
     * 文件的原始大小（未编码前）
     * 用于显示和容量限制
     */
    private Integer fileSize;

    /**
     * 文件编码方式
     * utf-8: 文本文件，直接存储
     * base64: 二进制文件或复杂格式，进行base64编码
     */
    private String encoding;

    /**
     * 创建时间
     * 文件首次上传到技能包的时刻
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 文件最后一次修改的时刻
     */
    private LocalDateTime updatedAt;
}
