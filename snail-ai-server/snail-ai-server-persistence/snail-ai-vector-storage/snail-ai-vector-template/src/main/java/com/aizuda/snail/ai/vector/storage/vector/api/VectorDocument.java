package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 向量文档模型，承载写入向量库的一条数据：唯一标识、原始文本、向量表示及附加元数据。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorDocument {

    /**
     * 文档唯一标识
     */
    private String id;

    /**
     * 原始文本内容
     */
    private String content;

    /**
     * 文本对应的向量表示（嵌入）
     */
    private float[] embedding;

    /**
     * 附加元数据（如 agentId、来源、分片信息等），用于过滤与回溯
     */
    private Map<String, Object> metadata;
}
