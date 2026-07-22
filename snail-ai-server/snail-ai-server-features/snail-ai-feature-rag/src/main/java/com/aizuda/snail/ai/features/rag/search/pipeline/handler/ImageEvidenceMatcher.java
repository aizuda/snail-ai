package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.common.dto.rag.SearchResultImage;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;

/**
 * 图片证据字段匹配器，用于检索召回和结果图片回填共用同一套命中规则。
 *
 * @author opensnail
 * @since 2026-07-22
 */
@Component
public class ImageEvidenceMatcher {

    public static final String MATCH_TYPE_DIRECT = "DIRECT_MATCH";
    public static final String FIELD_FIGURE_NO = "figureNo";
    public static final String FIELD_FIGURE_TITLE = "figureTitle";
    public static final String FIELD_SECTION_TITLE = "sectionTitle";
    public static final String FIELD_CAPTION = "caption";
    public static final String FIELD_OCR_TEXT = "ocrText";

    private static final int MIN_TERM_LENGTH = 2;
    private static final int MAX_CJK_TERM_LENGTH = 6;
    private static final int STRONG_MATCH_WEIGHT = 3;
    private static final String DEFAULT_FIELD_LABEL = "图片内容";
    private static final Pattern LATIN_WORD_PATTERN = Pattern.compile("[A-Za-z0-9]{2,}");
    private static final Pattern NON_TEXT_PATTERN = Pattern.compile("[\\p{Punct}\\s，。！？、：；（）【】《》]+");

    public ImageEvidence match(RagDocumentImagePO image, String query) {
        if (image == null) {
            return ImageEvidence.empty();
        }
        return match(image, extractQueryTerms(query));
    }

    public ImageEvidence match(RagDocumentImagePO image, List<String> queryTerms) {
        if (image == null || CollUtil.isEmpty(queryTerms)) {
            return ImageEvidence.empty();
        }
        List<String> matchedTerms = new ArrayList<>();
        List<String> matchedFields = new ArrayList<>();
        int score = 0;
        score += scoreField(FIELD_FIGURE_NO, image.getFigureNo(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_FIGURE_TITLE, image.getFigureTitle(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_SECTION_TITLE, image.getSectionTitle(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_CAPTION, image.getCaption(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_OCR_TEXT, image.getOcrText(), queryTerms, matchedTerms, matchedFields);
        return new ImageEvidence(
                score,
                matchedTerms.stream().distinct().toList(),
                matchedFields.stream().distinct().toList());
    }

    public ImageEvidence match(SearchResultImage image, List<String> queryTerms) {
        if (image == null || CollUtil.isEmpty(queryTerms)) {
            return ImageEvidence.empty();
        }
        List<String> matchedTerms = new ArrayList<>();
        List<String> matchedFields = new ArrayList<>();
        int score = 0;
        score += scoreField(FIELD_FIGURE_NO, image.getFigureNo(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_FIGURE_TITLE, image.getFigureTitle(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_SECTION_TITLE, image.getSectionTitle(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_CAPTION, image.getCaption(), queryTerms, matchedTerms, matchedFields);
        score += scoreField(FIELD_OCR_TEXT, image.getOcrText(), queryTerms, matchedTerms, matchedFields);
        return new ImageEvidence(
                score,
                matchedTerms.stream().distinct().toList(),
                matchedFields.stream().distinct().toList());
    }

    public String buildDirectMatchReason(ImageEvidence evidence) {
        if (evidence == null) {
            return DEFAULT_FIELD_LABEL + " 命中";
        }
        String fieldLabel = evidence.fields().isEmpty()
                ? DEFAULT_FIELD_LABEL
                : fieldLabel(evidence.fields().get(0));
        if (CollUtil.isEmpty(evidence.terms())) {
            return fieldLabel + " 命中";
        }
        String bestTerm = evidence.terms().stream()
                .max((left, right) -> Integer.compare(left.length(), right.length()))
                .orElse(evidence.terms().get(0));
        return fieldLabel + " 命中：" + bestTerm;
    }

    public List<String> extractQueryTerms(String query) {
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

    private String fieldLabel(String field) {
        return switch (field) {
            case FIELD_FIGURE_NO -> "图号";
            case FIELD_FIGURE_TITLE -> "图标题";
            case FIELD_SECTION_TITLE -> "章节标题";
            case FIELD_CAPTION -> "图注";
            case FIELD_OCR_TEXT -> "OCR";
            default -> DEFAULT_FIELD_LABEL;
        };
    }

    private String normalize(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return NON_TEXT_PATTERN.matcher(text.toLowerCase()).replaceAll("");
    }

    public record ImageEvidence(int score, List<String> terms, List<String> fields) {

        public static ImageEvidence empty() {
            return new ImageEvidence(0, List.of(), List.of());
        }
    }
}
