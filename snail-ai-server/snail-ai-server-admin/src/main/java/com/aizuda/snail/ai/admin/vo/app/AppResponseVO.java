package com.aizuda.snail.ai.admin.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
