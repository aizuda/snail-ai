package com.aizuda.snail.ai.admin.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 接入应用响应对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppResponseVO {
    /** 主键 ID */
    private Long id;
    /** 应用 ID */
    private String appId;
    /** 应用名称 */
    private String appName;
    /** 应用描述 */
    private String description;
    /** 访问令牌 */
    private String token;
    /** 路由策略 */
    private String routeStrategy;
    /** 状态 */
    private Integer status;
    /** 在线节点数 */
    private Integer onlineNodes;
    /** 创建时间 */
    private LocalDateTime createDt;
}
