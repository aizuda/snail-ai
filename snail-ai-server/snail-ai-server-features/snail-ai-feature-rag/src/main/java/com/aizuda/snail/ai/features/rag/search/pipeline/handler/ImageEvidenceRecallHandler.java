package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.enums.RagChunkSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import com.aizuda.snail.ai.search.storage.search.constant.SearchMetadataKeys;

/**
 * 图片证据辅助召回：当向量/BM25 没召回图片 chunk，但图片标题、图注或可索引 OCR 能直接命中查询时，
 * 将对应图片 chunk 补入候选集，继续交给重排和收尾阶段处理。
 *
 * @author opensnail
 * @since 2026-07-22
 */
@Slf4j
@Component
@Order(55)
@RequiredArgsConstructor
public class ImageEvidenceRecallHandler implements RagSearchHandler {

    private static final int DEFAULT_RESULT_COUNT = 5;
    private static final int MAX_OCR_CONTENT_LENGTH = 1200;
    private static final double EVIDENCE_BASE_SCORE = 0.75D;
    private static final double EVIDENCE_SCORE_STEP = 0.05D;
    private static final double EVIDENCE_MAX_SCORE = 0.95D;
    private static final String CONTENT_TITLE = "图片证据";
    private static final String LABEL_DOCUMENT = "文档";
    private static final String LABEL_FIGURE_NO = "图号";
    private static final String LABEL_FIGURE_TITLE = "图标题";
    private static final String LABEL_SECTION_TITLE = "章节";
    private static final String LABEL_CAPTION = "图注";
    private static final String LABEL_OCR_TEXT = "OCR";
    private static final String METADATA_IMAGE_MATCH_TYPE = "imageMatchType";
    private static final String METADATA_IMAGE_MATCH_REASON = "imageMatchReason";

    private final RagDocumentImageMapper ragDocumentImageMapper;
    private final ImageEvidenceMatcher imageEvidenceMatcher;

    @Override
    public void handle(RagSearchContext ctx) {
        if (!canRecall(ctx)) {
            return;
        }
        List<RagDocumentImagePO> images = ragDocumentImageMapper.selectList(
                new LambdaQueryWrapper<RagDocumentImagePO>()
                        .eq(RagDocumentImagePO::getRagId, ctx.getKnowledge().getId())
                        .isNotNull(RagDocumentImagePO::getChunkId)
                        .orderByAsc(RagDocumentImagePO::getSourcePage)
                        .orderByAsc(RagDocumentImagePO::getImageIndex)
                        .orderByAsc(RagDocumentImagePO::getId));
        if (CollUtil.isEmpty(images)) {
            return;
        }

        List<SearchResult> existingResults = ctx.getResults() == null ? List.of() : ctx.getResults();
        Set<Long> existingChunkIds = existingResults.stream()
                .map(SearchResult::getChunkId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, Set::add, Set::addAll);
        Set<Long> recalledChunkIds = new HashSet<>();
        int limit = resolveResultCount(ctx.getSearchParams());

        List<SearchResult> recalledResults = images.stream()
                .map(image -> toRecallResult(image, ctx.getQuery(), existingChunkIds, recalledChunkIds))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SearchResult::getScore).reversed())
                .limit(limit)
                .toList();
        if (recalledResults.isEmpty()) {
            return;
        }

        List<SearchResult> combinedResults = new ArrayList<>(existingResults.size() + recalledResults.size());
        combinedResults.addAll(existingResults);
        combinedResults.addAll(recalledResults);
        combinedResults.sort(Comparator.comparing(SearchResult::getScore).reversed());
        ctx.setResults(combinedResults);
        log.debug("图片证据辅助召回完成 ragId={}, recalled={}", ctx.getKnowledge().getId(), recalledResults.size());
    }

    private boolean canRecall(RagSearchContext ctx) {
        if (ctx == null || ctx.getKnowledge() == null || ctx.getKnowledge().getId() == null) {
            return false;
        }
        return StrUtil.isNotBlank(ctx.getQuery());
    }

    private SearchResult toRecallResult(RagDocumentImagePO image, String query, Set<Long> existingChunkIds,
                                        Set<Long> recalledChunkIds) {
        if (image.getChunkId() == null
                || existingChunkIds.contains(image.getChunkId())
                || !recalledChunkIds.add(image.getChunkId())) {
            return null;
        }
        ImageEvidenceMatcher.ImageEvidence evidence = imageEvidenceMatcher.match(image, query);
        if (evidence.score() <= 0) {
            recalledChunkIds.remove(image.getChunkId());
            return null;
        }
        return SearchResult.builder()
                .chunkId(image.getChunkId())
                .content(buildEvidenceContent(image))
                .score(toSearchScore(evidence.score()))
                .documentId(image.getDocumentId())
                .documentName(image.getDocumentName())
                .metadata(Map.of(
                        SearchMetadataKeys.CHUNK_ID, image.getChunkId(),
                        SearchMetadataKeys.SOURCE_TYPE, RagChunkSourceTypeEnum.IMAGE.getValue(),
                        METADATA_IMAGE_MATCH_TYPE, ImageEvidenceMatcher.MATCH_TYPE_DIRECT,
                        METADATA_IMAGE_MATCH_REASON,
                        imageEvidenceMatcher.buildDirectMatchReason(evidence)))
                .build();
    }

    private String buildEvidenceContent(RagDocumentImagePO image) {
        List<String> lines = new ArrayList<>();
        lines.add(CONTENT_TITLE);
        appendLine(lines, LABEL_DOCUMENT, image.getDocumentName());
        appendLine(lines, LABEL_FIGURE_NO, image.getFigureNo());
        appendLine(lines, LABEL_FIGURE_TITLE, image.getFigureTitle());
        appendLine(lines, LABEL_SECTION_TITLE, image.getSectionTitle());
        appendLine(lines, LABEL_CAPTION, image.getCaption());
        appendLine(lines, LABEL_OCR_TEXT, StrUtil.maxLength(image.getOcrText(), MAX_OCR_CONTENT_LENGTH));
        return String.join("\n", lines);
    }

    private void appendLine(List<String> lines, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        lines.add(label + "：" + value);
    }

    private double toSearchScore(int evidenceScore) {
        return Math.min(EVIDENCE_MAX_SCORE, EVIDENCE_BASE_SCORE + evidenceScore * EVIDENCE_SCORE_STEP);
    }

    private int resolveResultCount(RagConfigDO.SearchParams searchParams) {
        if (searchParams == null || searchParams.getResultCount() == null || searchParams.getResultCount() <= 0) {
            return DEFAULT_RESULT_COUNT;
        }
        return searchParams.getResultCount();
    }
}
