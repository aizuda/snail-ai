package com.aizuda.snail.ai.admin.vo.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class McpServerResponseVO {

    private Long id;

    private String name;

    private String description;

    private Integer transportType;

    private String baseUri;

    private String endpoint;

    private String command;

    private List<String> args;

    private Map<String, String> envVars;

    private String version;

    private Integer authType;

    private Integer status;

    private List<String> capabilities;

    private LocalDateTime lastConnectDt;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
