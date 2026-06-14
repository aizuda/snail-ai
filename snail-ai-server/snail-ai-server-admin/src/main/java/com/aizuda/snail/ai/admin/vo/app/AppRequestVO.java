package com.aizuda.snail.ai.admin.vo.app;

import lombok.Data;

@Data
public class AppRequestVO {
    private String appId;
    private String appName;
    private String description;
    private String routeStrategy;
}
