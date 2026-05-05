package com.aizuda.snail.ai.model.model.rerank;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.model.Model;

import java.util.List;

/**
 * 重排模型接口
 */
public interface RerankModel extends Model {

    /**
     * 对候选文档进行重排
     */
    RerankResponse rerank(RerankDTO dto) throws ModelCallException;

    /**
     * 重排请求 DTO
     *
     * @param query     查询文本
     * @param documents 候选文档内容列表
     * @param topN      返回前 N 个结果
     */
    record RerankDTO(String query, List<String> documents, Integer topN) {
    }
}
