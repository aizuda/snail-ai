package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.dto.rag.SearchResultImage;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;

/**
 * Enriches RAG search hits with images associated to the hit chunk.
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Component
@RequiredArgsConstructor
public class SearchResultImageEnricher {

    private static final int MAX_IMAGES_PER_RESULT = 3;
    private static final int MIN_TERM_LENGTH = 2;
    private static final int MAX_CJK_TERM_LENGTH = 6;
    private static final int STRONG_MATCH_WEIGHT = 3;
    private static final String MATCH_TYPE_DIRECT = "DIRECT_MATCH";
    private static final String MATCH_TYPE_CONTEXT = "CONTEXT_MATCH";
    private static final String CONTEXT_MATCH_REASON = "\u5173\u8054\u5230\u547d\u4e2d\u7684\u6587\u672c\u7247\u6bb5";
    private static final String FIELD_FIGURE_NO = "figureNo";
    private static final String FIELD_FIGURE_TITLE = "figureTitle";
    private static final String FIELD_CAPTION = "caption";
    private static final String FIELD_OCR_TEXT = "ocrText";
    private static final Pattern LATIN_WORD_PATTERN = Pattern.compile("[A-Za-z0-9]{2,}");
    private static final Pattern NON_TEXT_PATTERN = Pattern.compile("[\\p{Punct}\\s，。！？、：；（）【】《》]+");

    private final RagDocumentImageMapper ragDocumentImageMapper;

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
        List<String> queryTerms = extractQueryTerms(query);
        for (SearchResult result : results) {
            if (result.getChunkId() != null) {
                result.setImages(filterImages(imageMap.get(result.getChunkId()), queryTerms));
            }
        }
    }

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
        ImageEvidence evidence = matchImage(image, queryTerms);
        if (evidence.score() > 0) {
            image.setMatchType(MATCH_TYPE_DIRECT);
            image.setMatchScore(evidence.score());
            image.setMatchReason(buildDirectMatchReason(evidence));
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

    private ImageEvidence matchImage(SearchResultImage image, List<String> queryTerms) {
        if (CollUtil.isEmpty(queryTerms)) {
            return new ImageEvidence(0, List.of(), List.of());
        }
        List<String> matchedTerms = new ArrayList<>();
        List<String> matchedFields = new ArrayList<>();
        int score = 0;
        score += scoreField(FIELD_FIGURE_NO, image.getFigureNo(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_FIGURE_TITLE, image.getFigureTitle(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_CAPTION, image.getCaption(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_OCR_TEXT, image.getOcrText(), queryTerms, matchedTerms, matchedFields);
        return new ImageEvidence(
                score,
                matchedTerms.stream().distinct().toList(),
                matchedFields.stream().distinct().toList());
    }

    private int scoreField(String field, String value, List<String> queryTerms, List<String> matchedTerms,
                           List<String> matchedFields) {
        String normalized = normalize(value);
        if (StrUtil.isBlank(normalized)) {
            return 0;
        }
        int score = 0;
        for (String term : queryTerms) {
            if (normalized.contains(term)) {
                score += STRONG_MATCH_WEIGHT;
                matchedTerms.add(term);
                matchedFields.add(field);
            }
        }
        return score;
    }

    private String buildDirectMatchReason(ImageEvidence evidence) {
        String fieldLabel = evidence.fields().isEmpty()
                ? "\u56fe\u7247\u5185\u5bb9"
                : fieldLabel(evidence.fields().get(0));
        if (CollUtil.isEmpty(evidence.terms())) {
            return fieldLabel + "\u547d\u4e2d";
        }
        return fieldLabel + "\u547d\u4e2d\uff1a" + evidence.terms().get(0);
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case FIELD_FIGURE_NO -> "\u56fe\u53f7";
            case FIELD_FIGURE_TITLE -> "\u56fe\u6807\u9898";
            case FIELD_CAPTION -> "\u56fe\u6ce8";
            case FIELD_OCR_TEXT -> "OCR";
            default -> "\u56fe\u7247\u5185\u5bb9";
        };
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
        return MATCH_TYPE_DIRECT.equals(image.getMatchType());
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int nullToMax(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private List<String> extractQueryTerms(String query) {
        String normalized = normalize(query);
        if (normalized.length() < MIN_TERM_LENGTH) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        terms.add(normalized);

        int maxLength = Math.min(MAX_CJK_TERM_LENGTH, normalized.length());
        for (int length = MIN_TERM_LENGTH; length <= maxLength; length++) {
            for (int i = 0; i + length <= normalized.length(); i++) {
                terms.add(normalized.substring(i, i + length));
            }
        }

        Matcher matcher = LATIN_WORD_PATTERN.matcher(query);
        while (matcher.find()) {
            terms.add(matcher.group().toLowerCase());
        }
        return terms.stream()
                .filter(term -> term.length() >= MIN_TERM_LENGTH)
                .distinct()
                .toList();
    }

    private String normalize(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return NON_TEXT_PATTERN.matcher(text.toLowerCase()).replaceAll("");
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

    private record ImageEvidence(int score, List<String> terms, List<String> fields) {
    }
}
