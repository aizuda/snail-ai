package com.aizuda.snail.ai.admin.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 接入应用客户端节点视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientNodeVO {
    /** 节点 ID */
    private Long id;
    /** 应用 ID */
    private String appId;
    /** 应用名称 */
    private String appName;
    /** 主机 ID */
    private String hostId;
    /** 主机 IP */
    private String hostIp;
    /** gRPC 端口 */
    private Integer grpcPort;
    /** 最大并发数 */
    private Integer maxConcurrent;
    /** 活跃会话数 */
    private Integer activeChats;
    /** 节点标签 */
    private Map<String, String> labels;
    /** 过期时间 */
    private LocalDateTime expireDt;
    /** 是否在线 */
    private Boolean online;
}
