package com.aizuda.snail.ai.admin.task;

import com.aizuda.snail.ai.features.rag.enums.RagDocumentStatus;
import com.aizuda.snail.ai.features.rag.pipeline.DocumentPipeline;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档处理定时任务
 * 扫描 PENDING 状态的文档并逐个处理（解析、切片、向量化）
 *
 * @author opensnail
 * @date 2026-04-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingTask {

    private final RagDocumentMapper ragDocumentMapper;
    private final DocumentPipeline documentPipeline;

    @Scheduled(fixedDelay = 10_000)
    public void processPendingDocuments() {
        List<RagDocumentPO> pendingDocs = ragDocumentMapper.selectList(
                new LambdaQueryWrapper<RagDocumentPO>()
                        .eq(RagDocumentPO::getStatus, RagDocumentStatus.PENDING.getStatus())
                        .orderByAsc(RagDocumentPO::getCreateDt)
                        .last("LIMIT 5"));

        if (pendingDocs.isEmpty()) {
            return;
        }

        log.info("Found {} pending documents to process", pendingDocs.size());

        for (RagDocumentPO doc : pendingDocs) {
            try {
                log.info("Processing pending document: [{}] {}", doc.getId(), doc.getName());
                documentPipeline.processDocument(doc.getId());
            } catch (Exception e) {
                log.error("Failed to process document: [{}]", doc.getId(), e);
            }
        }
    }
}
