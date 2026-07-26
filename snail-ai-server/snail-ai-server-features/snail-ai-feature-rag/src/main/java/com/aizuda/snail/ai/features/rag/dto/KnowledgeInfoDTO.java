package com.aizuda.snail.ai.features.rag.dto;

/**
 * 知识库信息：名称、描述及文档 / 切块统计数量。
 *
 * @author opensnail
 * @since 2026-05-07
 */
public class KnowledgeInfoDTO {
    /**
     * 知识库 ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 文档数量
     */
    private Integer documentCount;

    /**
     * 切块数量
     */
    private Integer chunkCount;
}
