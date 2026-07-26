package com.aizuda.snail.ai.admin.vo.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资源响应对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourceResponseVO {
    /** 资源 ID */
    private Long id;
    /** 存储键 */
    private String storageKey;
    /** 原始文件名 */
    private String originalName;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 文件 MIME 类型 */
    private String mimeType;
    /** 存储类型 */
    private String storageType;
    /** 访问地址 */
    private String accessUrl;
    /** 业务类型 */
    private String bizType;
    /** 业务 ID */
    private Long bizId;
    /** 创建人 ID */
    private Long creatorId;
    /** 创建时间 */
    private LocalDateTime createDt;
}
