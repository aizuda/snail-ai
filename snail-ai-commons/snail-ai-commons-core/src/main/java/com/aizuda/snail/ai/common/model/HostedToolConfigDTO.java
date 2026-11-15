package com.aizuda.snail.ai.common.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Responses Hosted Tool 配置，通过 {@code type} 区分各工具的专属字段。
 *
 * @author opensnail
 * @since 2026-11-14
 */
@Data
public class HostedToolConfigDTO {

    private String type;

    // Web Search
    private String searchContextSize;
    private List<String> allowedDomains;

    // File Search
    private List<String> vectorStoreIds;
    private Integer maxNumResults;

    // Code Interpreter
    private String containerId;

    // Image Generation
    private String model;
    private String size;

    // Provider extension
    private Map<String, Object> tool;
}
