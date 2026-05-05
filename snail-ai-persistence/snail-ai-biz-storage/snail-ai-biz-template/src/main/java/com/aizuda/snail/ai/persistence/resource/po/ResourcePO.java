package com.aizuda.snail.ai.persistence.resource.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资源文件持久化对象
 * 表: snail_ai_resource
 *
 * 表示系统中的文件资源
 * 支持多种存储后端和业务类型的关联
 * 用于管理头像、文件上传、素材库等
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_resource")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourcePO {

    /**
     * 资源ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 存储key
     * 在存储后端中的唯一标识
     * 本地存储: 相对路径或哈希值
     * OSS/S3: 对象key
     */
    private String storageKey;

    /**
     * 原始文件名
     * 用户上传时的文件名
     * 用于下载时显示原始名称
     */
    private String originalName;

    /**
     * 文件大小 (字节)
     * 文件的实际大小
     */
    private Long fileSize;

    /**
     * MIME类型
     * 文件的媒体类型
     * 例如: application/pdf, image/png, text/plain
     */
    private String mimeType;

    /**
     * 存储类型
     * LOCAL: 本地存储
     * OSS: 阿里云对象存储
     * S3: AWS S3
     * AZURE: 微软Azure
     */
    private String storageType;

    /**
     * 访问URL
     * 资源的外部访问链接
     * 用于前端直接访问或下载
     */
    private String accessUrl;

    /**
     * 业务类型
     * 该资源关联的业务类型
     * 例如: AGENT_AVATAR, USER_AVATAR, SKILL_FILE等
     */
    private String bizType;

    /**
     * 业务ID
     * 该资源关联的业务对象ID
     * 如bizType=AGENT_AVATAR，则为Agent ID
     */
    private Long bizId;

    /**
     * 创建者用户ID (外键)
     * 关联到 snail_ai_user.id
     * 上传或创建该资源的用户
     */
    private Long creatorId;

    /**
     * 创建时间
     * 资源首次上传/创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 资源最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
