package com.aizuda.snail.ai.persistence.openapi.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OpenAPI 接入用户：外部应用用户与平台用户的映射关系。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@TableName("sai_openapi_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpenApiUserPO {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接入应用 ID */
    private String appId;

    /** 对外开放的用户标识（openId） */
    private String openId;

    /** 关联的平台用户 ID */
    private Long platformUserId;

    /** 外部系统的用户 ID */
    private String externalId;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}
