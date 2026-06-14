package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class MarkdownParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "md".equalsIgnoreCase(fileType) || "markdown".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String raw = reader.lines().collect(Collectors.joining("\n"));
            return cleanMarkdown(raw);
        } catch (Exception e) {
            throw new SnailAiException("Failed to parse markdown", e);
        }
    }

    private String cleanMarkdown(String md) {
        String clean = md;
        // 去掉标题标记，保留标题文本
        clean = clean.replaceAll("(?m)^#+\\s*", "");
        // 去掉代码块围栏标记（```language），保留代码内容
        clean = clean.replaceAll("(?m)^```[a-zA-Z]*\\s*$", "");
        // 去掉行内代码反引号，保留代码文本
        clean = clean.replaceAll("`([^`]+)`", "$1");
        // 去掉加粗标记
        clean = clean.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        // 去掉斜体标记
        clean = clean.replaceAll("\\*(.*?)\\*", "$1");
        // 去掉图片语法，图片无文本语义
        clean = clean.replaceAll("!\\[.*?]\\(.*?\\)", "");
        // 去掉链接语法，保留链接文本
        clean = clean.replaceAll("\\[(.*?)]\\(.*?\\)", "$1");
        // 去掉引用标记符号，保留引用文本
        clean = clean.replaceAll("(?m)^>\\s*", "");
        // 去掉列表标记符号，保留列表文本
        clean = clean.replaceAll("(?m)^[-*+]\\s+", "");
        return clean;
    }
}
