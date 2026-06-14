package com.aizuda.snail.ai.features.rag.strategy.chunker;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 各 {@link ChunkStrategy} 共用的二级递归切分与工具方法，不再作为对外切片入口。
 */
@Slf4j
@Component
public class TokenAwareChunker {

    /**
     * 将段落数组按 maxTokens 递归切分。
     */
    public List<ChunkDTO> chunkParagraphs(String[] paragraphs, int maxTokens, int overlap) {
        DocumentSplitter splitter = DocumentSplitters.recursive(maxTokens, overlap);
        List<ChunkDTO> chunks = new ArrayList<>();

        for (int pIdx = 0; pIdx < paragraphs.length; pIdx++) {
            String paragraph = paragraphs[pIdx].trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            List<TextSegment> segments = splitter.split(Document.from(paragraph));
            for (int cIdx = 0; cIdx < segments.size(); cIdx++) {
                String text = segments.get(cIdx).text().trim();
                if (text.isEmpty()) {
                    continue;
                }
                chunks.add(ChunkDTO.builder()
                        .paragraphIndex(pIdx)
                        .chunkIndex(cIdx)
                        .content(text)
                        .tokenCount(estimateTokens(text))
                        .build());
            }
        }
        return chunks;
    }

    /**
     * delimiter 为 null 时按段落 \\n\\n；为普通字符串时按该串切分；若以 [ 开头则解析为 JSON 字符串数组，按任一匹配切分（长串优先）。
     */
    public List<String> resolveDelimiterList(String raw) {
        if (StrUtil.isBlank(raw)) {
            return List.of("\n\n");
        }
        String s = raw.trim();
        if (s.startsWith("[")) {
            try {
                List<String> list = JsonUtil.parseList(s, String.class);
                List<String> filtered = list.stream().filter(StrUtil::isNotBlank).map(String::trim).toList();
                if (filtered.isEmpty()) {
                    return List.of("\n\n");
                }
                ArrayList<String> sorted = new ArrayList<>(filtered);
                sorted.sort(Comparator.comparingInt(String::length).reversed());
                return sorted;
            } catch (Exception e) {
                log.warn("解析多分隔符 JSON 失败，按字面量切分: {}", e.getMessage());
                return List.of(s);
            }
        }
        return List.of(s);
    }

    /**
     * 按多个分隔符切分内容（正则或字面值）。
     */
    public String[] splitByAnyDelimiter(String content, List<String> delimiters) {
        if (delimiters.isEmpty()) {
            return new String[] { content };
        }
        if (delimiters.size() == 1) {
            return content.split(Pattern.quote(delimiters.get(0)));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < delimiters.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(Pattern.quote(delimiters.get(i)));
        }
        return Pattern.compile(sb.toString()).split(content);
    }

    /** CJK 感知的 token 估算 */
    public int estimateTokens(String text) {
        int cjkCount = 0;
        int asciiWordCount = 0;
        boolean inWord = false;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjkCount++;
                inWord = false;
            } else if (Character.isLetterOrDigit(c)) {
                if (!inWord) {
                    asciiWordCount++;
                    inWord = true;
                }
            } else {
                inWord = false;
            }
        }
        return (int) (cjkCount * 1.5) + asciiWordCount;
    }
}
