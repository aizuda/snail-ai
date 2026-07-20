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
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.dto.rag.SearchResultImage;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;

/**
 * RAG 检索结果图片回填器：为命中的 chunk 关联文档图片，按查询词与图号/图标题/图注/OCR 文本
 * 的匹配度打分排序，每条结果最多保留 {@value #MAX_IMAGES_PER_RESULT} 张图片。
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchResultImageEnricher {

    private static final int MAX_IMAGES_PER_RESULT = 3;
    private static final int MIN_TERM_LENGTH = 2;
    private static final int MAX_CJK_TERM_LENGTH = 6;
    private static final int STRONG_MATCH_WEIGHT = 3;
    private static final String MATCH_TYPE_DIRECT = "DIRECT_MATCH";
    private static final String MATCH_TYPE_CONTEXT = "CONTEXT_MATCH";
    private static final String CONTEXT_MATCH_REASON = "关联到命中的文本片段";
    private static final String FIELD_FIGURE_NO = "figureNo";
    private static final String FIELD_FIGURE_TITLE = "figureTitle";
    private static final String FIELD_CAPTION = "caption";
    private static final String FIELD_OCR_TEXT = "ocrText";
    private static final Pattern LATIN_WORD_PATTERN = Pattern.compile("[A-Za-z0-9]{2,}");
    private static final Pattern NON_TEXT_PATTERN = Pattern.compile("[\\p{Punct}\\s，。！？、：；（）【】《》]+");

    private final RagDocumentImageMapper ragDocumentImageMapper;

    /**
     * 为检索结果批量回填关联图片（一次查库，内存分组匹配）
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
        List<String> queryTerms = extractQueryTerms(query);
        for (SearchResult result : results) {
            if (result.getChunkId() != null) {
                result.setImages(filterImages(imageMap.get(result.getChunkId()), queryTerms));
            }
        }
        log.debug("检索结果图片回填完成, hitChunks={}, candidateImages={}", chunkIds.size(), images.size());
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
                ? "图片内容"
                : fieldLabel(evidence.fields().get(0));
        if (CollUtil.isEmpty(evidence.terms())) {
            return fieldLabel + "命中";
        }
        return fieldLabel + "命中：" + evidence.terms().get(0);
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case FIELD_FIGURE_NO -> "图号";
            case FIELD_FIGURE_TITLE -> "图标题";
            case FIELD_CAPTION -> "图注";
            case FIELD_OCR_TEXT -> "OCR";
            default -> "图片内容";
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

    /** 提取匹配词：整句 + 中文 2~6 字滑窗 n-gram + 拉丁字母/数字单词，用于图片证据字段的包含匹配 */
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
