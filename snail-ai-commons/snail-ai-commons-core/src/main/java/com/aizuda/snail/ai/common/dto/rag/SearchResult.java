package com.aizuda.snail.ai.common.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 检索结果条目，表示一次向量/全文检索命中的文档分片及其评分与归属信息。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResult {
    /**
     * 命中记录标识（向量库 / 搜索引擎内的主键）
     */
    private String id;
    /**
     * 命中的文档分片 ID
     */
    private Long chunkId;
    /**
     * 命中文本内容
     */
    private String content;
    /**
     * 相关性得分
     */
    private double score;
    /**
     * 所属文档 ID
     */
    private Long documentId;
    /**
     * 所属文档名称
     */
    private String documentName;
    /**
     * 附加元数据
     */
    private Map<String, Object> metadata;

    private List<SearchResultImage> images;
}
