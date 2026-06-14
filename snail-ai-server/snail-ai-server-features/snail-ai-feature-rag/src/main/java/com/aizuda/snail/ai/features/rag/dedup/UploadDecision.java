package com.aizuda.snail.ai.features.rag.dedup;

import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 单个文件的上传决策：决策类型 + 命中维度 + 冲突文档
 *
 * @author opensnail
 */
@Getter
@AllArgsConstructor
public class UploadDecision {

    private final UploadDecisionType type;

    private final DedupMatchType matchType;

    private final RagDocumentPO conflict;

    public static UploadDecision newFile() {
        return new UploadDecision(UploadDecisionType.NEW, DedupMatchType.NONE, null);
    }

    public static UploadDecision reject(DedupResult r) {
        return new UploadDecision(UploadDecisionType.REJECT, r.getMatchType(), r.getConflict());
    }

    public static UploadDecision skip(DedupResult r) {
        return new UploadDecision(UploadDecisionType.SKIP, r.getMatchType(), r.getConflict());
    }

    public static UploadDecision overwrite(DedupResult r) {
        return new UploadDecision(UploadDecisionType.OVERWRITE, r.getMatchType(), r.getConflict());
    }

    public boolean isNew() {
        return type == UploadDecisionType.NEW;
    }
}
