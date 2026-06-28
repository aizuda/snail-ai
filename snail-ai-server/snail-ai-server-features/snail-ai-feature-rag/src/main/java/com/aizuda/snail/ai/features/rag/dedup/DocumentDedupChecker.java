package com.aizuda.snail.ai.features.rag.dedup;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.features.rag.enums.DedupAction;
import com.aizuda.snail.ai.features.rag.enums.DedupStrategy;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 知识库文档去重判定器
 * <p>
 * 单一职责：根据策略查询冲突文档，并结合冲突动作给出最终决策。
 *
 * @author opensnail
 */
@Component
@RequiredArgsConstructor
public class DocumentDedupChecker {

    private final RagDocumentMapper ragDocumentMapper;

    /**
     * 按策略检查重复
     *
     * @param ragId       RAG ID
     * @param fileName    文件名
     * @param contentHash 文件内容 SHA-256
     * @param strategy    去重策略
     * @return 命中结果（含冲突文档）
     */
    public DedupResult check(Long ragId, String fileName, String contentHash, DedupStrategy strategy) {
        if (ragId == null || strategy == null || strategy == DedupStrategy.NONE) {
            return DedupResult.none();
        }

        RagDocumentPO byContent = null;
        if (strategy.matchesByContent() && StrUtil.isNotBlank(contentHash)) {
            byContent = ragDocumentMapper.selectOne(
                    new LambdaQueryWrapper<RagDocumentPO>()
                            .eq(RagDocumentPO::getRagId, ragId)
                            .eq(RagDocumentPO::getContentHash, contentHash)
                            .last("LIMIT 1"));
        }

        RagDocumentPO byName = null;
        if (strategy.matchesByName() && StrUtil.isNotBlank(fileName)) {
            byName = ragDocumentMapper.selectOne(
                    new LambdaQueryWrapper<RagDocumentPO>()
                            .eq(RagDocumentPO::getRagId, ragId)
                            .eq(RagDocumentPO::getName, fileName)
                            .last("LIMIT 1"));
        }

        if (byName != null && byContent != null && Objects.equals(byName.getId(), byContent.getId())) {
            return DedupResult.of(DedupMatchType.BOTH, byName);
        }
        // 同一冲突优先报告内容命中（更严格）；都存在则按 BOTH 处理（已在上面分支返回）
        if (byContent != null) {
            return DedupResult.of(DedupMatchType.BY_CONTENT, byContent);
        }
        if (byName != null) {
            return DedupResult.of(DedupMatchType.BY_NAME, byName);
        }
        return DedupResult.none();
    }

    /**
     * 把命中结果与冲突动作组合成最终决策
     */
    public UploadDecision decide(DedupResult result, DedupAction action) {
        if (!result.isHit()) {
            return UploadDecision.newFile();
        }
        DedupAction effective = action == null ? DedupAction.REJECT : action;
        return switch (effective) {
            case REJECT -> UploadDecision.reject(result);
            case SKIP -> UploadDecision.skip(result);
            case OVERWRITE -> UploadDecision.overwrite(result);
        };
    }
}
