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
 * 技能(Skill)信息持久化对象
 * 表: sai_skill
 *
 * 表示一个可被Agent执行的技能
 * 技能可以包含SKILL.md定义和支撑文件（脚本、参考资料等）
 * 支持从DB或文件系统/对象存储读取
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_skill")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkillPO {

    /**
     * 技能ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 技能名称
     * 供用户和Agent识别的技能显示名称
     */
    private String name;

    /**
     * 技能描述
     * 技能的功能和用途说明
     */
    private String description;

    /**
     * 文件名称
     * 原始上传文件的名称（通常为package.zip或skill.md）
     */
    private String fileName;

    /**
     * 文件路径 (绝对路径)
     * 技能文件在本地文件系统中的绝对路径
     * 当不使用对象存储时使用此字段
     */
    private String filePath;

    /**
     * 文件大小 (字节)
     * 压缩包或单个文件的大小
     */
    private Long fileSize;

    /**
     * 技能内容
     * SKILL.md定义的技能内容
     * 可直接存储在DB中，无需文件系统访问
     */
    private String skillContent;

    /**
     * 对象存储相对路径前缀
     * 当使用对象存储（S3/MinIO/OSS）时的存储路径
     * 格式示例: skills/123/ 或 skills/tech-skill-001/v2/
     * 为null时表示使用本地文件系统（filePath）
     */
    private String storagePath;

    /**
     * 版本号
     * 每次文件更新时自增
     * 用于缓存一致性校验和版本管理
     */
    private Long version;

    /**
     * 是否包含支撑文件
     * true: 包含scripts/、references/、examples/等附加文件
     * false: 仅有SKILL.md，可直接从DB读取，跳过文件系统访问
     * 影响性能和访问策略
     */
    private Boolean hasFiles;

    /**
     * 创建者用户ID (外键)
     * 关联到 sai_user.id
     * 该技能的创建人
     */
    private Long creatorId;

    /**
     * 创建时间
     * 技能首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 技能最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
