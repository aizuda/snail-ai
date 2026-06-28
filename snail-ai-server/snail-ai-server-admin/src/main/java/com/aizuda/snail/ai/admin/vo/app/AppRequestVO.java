package com.aizuda.snail.ai.admin.vo.app;

import lombok.Data;

/**
 * AppRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AppRequestVO {
    private String appId;
    private String appName;
    private String description;
    private String routeStrategy;
}
