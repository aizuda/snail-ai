package com.aizuda.snail.ai.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OpenAPI 消息记录
 *
 * @author opensnail
 * @since 2026-04-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiMessageVO {

    /**
     * 消息角色（user / assistant）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createDt;
}
