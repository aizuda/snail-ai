package com.aizuda.snail.ai.common.model;

import java.util.List;

/**
 * Rerank API 客户端接口
 * 封装对不同 rerank 服务提供商的 HTTP 调用
 */
public interface RerankApiClient {

    /**
     * 调用 rerank API
     *
     * @param query     查询文本
     * @param documents 候选文档列表
     * @param topN      返回前 N 个结果
     * @return 重排结果列表（按 score 降序）
     */
    List<RerankResultItem> rerank(String query, List<String> documents, int topN);

    /**
     * 单条重排结果
     */
    record RerankResultItem(int index, double score) {
    }
}
