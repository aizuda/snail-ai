package com.aizuda.snail.ai.admin.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AppResponseVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppResponseVO {
    private Long id;
    private String appId;
    private String appName;
    private String description;
    private String token;
    private String routeStrategy;
    private Integer status;
    private Integer onlineNodes;
    private LocalDateTime createDt;
}
