package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量写入请求（显式指定索引 / 集合名称，避免仅从 metadata 推导）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorAddRequest {

    /**
     * 索引或集合名称（必填）。
     * RAG：通常为 {@code {indexPrefix}_{ragId}}；
     * 记忆：{@code memory_agent_{agentId}}。
     */
    private String indexName;

    private List<VectorDocument> documents;
}
