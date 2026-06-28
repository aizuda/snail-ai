package com.aizuda.snail.ai.admin.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ClientNodeVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientNodeVO {
    private Long id;
    private String appId;
    private String appName;
    private String hostId;
    private String hostIp;
    private Integer grpcPort;
    private Integer maxConcurrent;
    private Integer activeChats;
    private Map<String, String> labels;
    private LocalDateTime expireDt;
    private Boolean online;
}
