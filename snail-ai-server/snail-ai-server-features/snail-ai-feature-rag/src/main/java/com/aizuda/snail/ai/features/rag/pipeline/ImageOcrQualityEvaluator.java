package com.aizuda.snail.ai.features.rag.pipeline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.hutool.core.util.StrUtil;

import org.springframework.stereotype.Component;

/**
 * Rule-based quality gate for image OCR text before it becomes searchable RAG content.
 *
 * @author opensnail
 * @since 2026-07-22
 */
@Component
public class ImageOcrQualityEvaluator {

    private static final int MIN_EFFECTIVE_CHAR_COUNT = 8;
    private static final double MIN_EFFECTIVE_CHAR_RATIO = 0.5;
    private static final double MAX_SINGLE_CHAR_LINE_RATIO = 0.6;
    private static final double MAX_REPEATED_LINE_RATIO = 0.5;

    public boolean isIndexable(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        String trimmedText = text.trim();
        int visibleCharCount = countVisibleChars(trimmedText);
        if (visibleCharCount == 0) {
            return false;
        }
        int effectiveCharCount = countEffectiveChars(trimmedText);
        if (effectiveCharCount < MIN_EFFECTIVE_CHAR_COUNT) {
            return false;
        }
        if ((double) effectiveCharCount / visibleCharCount < MIN_EFFECTIVE_CHAR_RATIO) {
            return false;
        }
        List<String> lines = trimmedText.lines()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
        if (hasTooManySingleCharLines(lines)) {
            return false;
        }
        return !hasTooManyRepeatedLines(lines);
    }

    private int countVisibleChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private int countEffectiveChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isEffectiveChar(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private boolean isEffectiveChar(char ch) {
        return Character.isLetterOrDigit(ch) || isCjkChar(ch);
    }

    private boolean isCjkChar(char ch) {
        Character.UnicodeScript script = Character.UnicodeScript.of(ch);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private boolean hasTooManySingleCharLines(List<String> lines) {
        if (lines.isEmpty()) {
            return false;
        }
        long singleCharLineCount = lines.stream()
                .filter(line -> countEffectiveChars(line) <= 1)
                .count();
        return (double) singleCharLineCount / lines.size() > MAX_SINGLE_CHAR_LINE_RATIO;
    }

    private boolean hasTooManyRepeatedLines(List<String> lines) {
        if (lines.size() <= 1) {
            return false;
        }
        Set<String> distinctLines = new HashSet<>(lines);
        int repeatedLineCount = lines.size() - distinctLines.size();
        return (double) repeatedLineCount / lines.size() > MAX_REPEATED_LINE_RATIO;
    }
}
