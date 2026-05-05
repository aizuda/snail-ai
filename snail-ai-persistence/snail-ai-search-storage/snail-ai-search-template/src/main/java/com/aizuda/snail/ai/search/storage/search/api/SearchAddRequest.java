package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索文档写入请求（显式指定 indexName，避免从 metadata 隐式推导）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchAddRequest {

    private String indexName;

    private List<SearchDocument> documents;
}
