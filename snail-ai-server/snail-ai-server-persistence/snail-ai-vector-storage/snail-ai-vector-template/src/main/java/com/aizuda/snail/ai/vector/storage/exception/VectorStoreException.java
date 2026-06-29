package com.aizuda.snail.ai.vector.storage.exception;

import com.aizuda.snail.ai.common.execption.BaseSnailAiException;

/**
 * 向量库操作异常，封装向量存储（Milvus/Elasticsearch/PGVector 等）读写过程中的失败。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class VectorStoreException extends BaseSnailAiException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
