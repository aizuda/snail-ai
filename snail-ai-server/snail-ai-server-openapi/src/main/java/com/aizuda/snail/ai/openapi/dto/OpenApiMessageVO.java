package com.aizuda.snail.ai.openapi.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI 消息记录
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiMessageVO {

    private String role;

    private String content;

    private Integer status;

    private LocalDateTime createDt;
}
