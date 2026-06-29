package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索文档写入请求（显式指定 indexName，避免从 metadata 隐式推导）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchAddRequest {

    /** 目标索引名 */
    private String indexName;

    /** 待写入的文档列表 */
    private List<SearchDocument> documents;
}
