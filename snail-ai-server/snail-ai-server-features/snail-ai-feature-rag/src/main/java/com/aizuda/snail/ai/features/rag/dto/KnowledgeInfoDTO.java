package com.aizuda.snail.ai.features.rag.dto;

/**
 * 知识库信息：名称、描述及文档 / 切块统计数量。
 *
 * @author opensnail
 * @date 2026-05-07
 */
public class KnowledgeInfoDTO {
    private Long id;
    private String name;
    private String description;
    private Integer documentCount;
    private Integer chunkCount;
}
