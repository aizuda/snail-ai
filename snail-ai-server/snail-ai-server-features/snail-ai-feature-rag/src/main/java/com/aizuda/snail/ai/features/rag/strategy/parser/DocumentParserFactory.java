package com.aizuda.snail.ai.features.rag.strategy.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析器工厂：按文件类型从已注册的 {@link DocumentParser} 中选择匹配实现。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    /**
     * 按文件类型选择解析器，未找到匹配实现时抛出 {@link IllegalArgumentException}。
     */
    public DocumentParser getParser(String fileType) {
        return parsers.stream()
                .filter(p -> p.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + fileType));
    }
}
