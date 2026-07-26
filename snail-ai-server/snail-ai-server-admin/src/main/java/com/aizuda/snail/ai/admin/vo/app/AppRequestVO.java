package com.aizuda.snail.ai.admin.vo.app;

import lombok.Data;

/**
 * 接入应用新增/更新请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class AppRequestVO {
    /** 应用 ID */
    private String appId;
    /** 应用名称 */
    private String appName;
    /** 应用描述 */
    private String description;
    /** 路由策略 */
    private String routeStrategy;
}
