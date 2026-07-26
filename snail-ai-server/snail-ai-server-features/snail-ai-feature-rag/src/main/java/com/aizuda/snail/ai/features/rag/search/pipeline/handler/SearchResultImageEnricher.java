package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.dto.rag.SearchResultImage;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;

/**
 * RAG 检索结果图片回填器：为命中的 chunk 关联文档图片，按查询词与图号/图标题/章节标题/图注/OCR 文本
 * 的匹配度打分排序，每条结果最多保留 {@value #MAX_IMAGES_PER_RESULT} 张图片。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-07-03
 */
@Slf4j
@Component
public class SearchResultImageEnricher {

    private static final int MAX_IMAGES_PER_RESULT = 3;
    private static final String MATCH_TYPE_CONTEXT = "CONTEXT_MATCH";
    private static final String CONTEXT_MATCH_REASON = "关联到命中的文本片段";

    private final RagDocumentImageMapper ragDocumentImageMapper;
    private final ImageEvidenceMatcher imageEvidenceMatcher;

    @Autowired
    public SearchResultImageEnricher(RagDocumentImageMapper ragDocumentImageMapper,
                                     ImageEvidenceMatcher imageEvidenceMatcher) {
        this.ragDocumentImageMapper = ragDocumentImageMapper;
        this.imageEvidenceMatcher = imageEvidenceMatcher;
    }

    public SearchResultImageEnricher(RagDocumentImageMapper ragDocumentImageMapper) {
        this(ragDocumentImageMapper, new ImageEvidenceMatcher());
    }

    /**
     * 为检索结果批量回填关联图片（一次查库，内存分组匹配）。
     *
     * @param results 检索命中结果，图片直接回填到每条结果的 images 字段
     * @param query   用户查询，用于图片证据匹配打分
     */
    public void enrich(List<SearchResult> results, String query) {
        List<Long> chunkIds = results.stream()
                .map(SearchResult::getChunkId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (chunkIds.isEmpty()) {
            return;
        }
        List<RagDocumentImagePO> images = ragDocumentImageMapper.selectList(
                new LambdaQueryWrapper<RagDocumentImagePO>()
                        .in(RagDocumentImagePO::getChunkId, chunkIds)
                        .orderByAsc(RagDocumentImagePO::getSourcePage)
                        .orderByAsc(RagDocumentImagePO::getImageIndex)
                        .orderByAsc(RagDocumentImagePO::getId));
        if (CollUtil.isEmpty(images)) {
            return;
        }
        Map<Long, List<SearchResultImage>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        RagDocumentImagePO::getChunkId,
                        Collectors.mapping(this::toDTO, Collectors.toList())));
        List<String> queryTerms = imageEvidenceMatcher.extractQueryTerms(query);
        for (SearchResult result : results) {
            if (result.getChunkId() != null) {
                result.setImages(filterImages(imageMap.get(result.getChunkId()), queryTerms));
            }
        }
        log.debug("检索结果图片回填完成 hitChunks={}, candidateImages={}", chunkIds.size(), images.size());
    }

    /** 为图片打匹配分并排序（直接命中优先、分数降序、页码升序），截取前 N 张 */
    private List<SearchResultImage> filterImages(List<SearchResultImage> images, List<String> queryTerms) {
        if (CollUtil.isEmpty(images)) {
            return null;
        }
        return images.stream()
                .peek(image -> applyMatchMetadata(image, queryTerms))
                .sorted(this::compareImages)
                .limit(MAX_IMAGES_PER_RESULT)
                .toList();
    }

    private void applyMatchMetadata(SearchResultImage image, List<String> queryTerms) {
        ImageEvidenceMatcher.ImageEvidence evidence = imageEvidenceMatcher.match(image, queryTerms);
        if (evidence.score() > 0) {
            image.setMatchType(ImageEvidenceMatcher.MATCH_TYPE_DIRECT);
            image.setMatchScore(evidence.score());
            image.setMatchReason(imageEvidenceMatcher.buildDirectMatchReason(evidence));
            image.setMatchedTerms(evidence.terms());
            image.setMatchedFields(evidence.fields());
            return;
        }
        image.setMatchType(MATCH_TYPE_CONTEXT);
        image.setMatchScore(0);
        image.setMatchReason(CONTEXT_MATCH_REASON);
        image.setMatchedTerms(List.of());
        image.setMatchedFields(List.of());
    }

    private int compareImages(SearchResultImage left, SearchResultImage right) {
        int typeCompare = Boolean.compare(isDirect(right), isDirect(left));
        if (typeCompare != 0) {
            return typeCompare;
        }
        int scoreCompare = Integer.compare(nullToZero(right.getMatchScore()), nullToZero(left.getMatchScore()));
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        int pageCompare = Integer.compare(nullToMax(left.getSourcePage()), nullToMax(right.getSourcePage()));
        if (pageCompare != 0) {
            return pageCompare;
        }
        return Integer.compare(nullToMax(left.getImageIndex()), nullToMax(right.getImageIndex()));
    }

    private boolean isDirect(SearchResultImage image) {
        return ImageEvidenceMatcher.MATCH_TYPE_DIRECT.equals(image.getMatchType());
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int nullToMax(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private SearchResultImage toDTO(RagDocumentImagePO po) {
        return SearchResultImage.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .documentId(po.getDocumentId())
                .chunkId(po.getChunkId())
                .resourceId(po.getResourceId())
                .imageIndex(po.getImageIndex())
                .imageUrl(po.getImageUrl())
                .caption(po.getCaption())
                .figureNo(po.getFigureNo())
                .figureTitle(po.getFigureTitle())
                .sectionTitle(po.getSectionTitle())
                .sourcePage(po.getSourcePage())
                .documentName(po.getDocumentName())
                .ocrText(po.getOcrText())
                .build();
    }
}
