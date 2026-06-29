package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 全文检索删除请求，指定索引下按文档 ID 批量删除。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchDeleteRequest {

    /**
     * 目标索引名
     */
    private String indexName;

    /**
     * 待删除的文档 ID 列表
     */
    private List<String> ids;
}
