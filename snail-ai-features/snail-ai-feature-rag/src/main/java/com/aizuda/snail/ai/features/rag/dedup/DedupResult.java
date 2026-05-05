package com.aizuda.snail.ai.features.rag.dedup;

import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 去重检查结果：是否命中、命中维度、冲突的旧文档
 *
 * @author opensnail
 */
@Getter
@AllArgsConstructor
public class DedupResult {

    private final DedupMatchType matchType;

    private final RagDocumentPO conflict;

    public static DedupResult none() {
        return new DedupResult(DedupMatchType.NONE, null);
    }

    public static DedupResult of(DedupMatchType type, RagDocumentPO conflict) {
        return new DedupResult(type, conflict);
    }

    public boolean isHit() {
        return matchType != DedupMatchType.NONE && conflict != null;
    }
}
