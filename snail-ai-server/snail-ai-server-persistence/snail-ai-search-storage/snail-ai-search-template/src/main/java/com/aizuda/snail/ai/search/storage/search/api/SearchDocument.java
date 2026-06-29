package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 全文检索文档模型，承载写入搜索引擎的一条数据：唯一标识、原文内容及附加元数据。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchDocument {

    /**
     * 文档唯一标识
     */
    private String id;

    /**
     * 原始文本内容
     */
    private String content;

    /**
     * 附加元数据（如 indexName、ragId、documentId 等），用于过滤与回溯
     */
    private Map<String, Object> metadata;
}
