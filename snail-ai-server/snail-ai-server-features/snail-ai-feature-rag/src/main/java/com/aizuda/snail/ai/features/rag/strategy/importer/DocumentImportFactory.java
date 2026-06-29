package com.aizuda.snail.ai.features.rag.strategy.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档导入策略工厂：按来源类型从已注册的 {@link DocumentImportStrategy} 中选择匹配实现。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class DocumentImportFactory {

    private final List<DocumentImportStrategy> strategies;

    /**
     * 按来源类型选择导入策略，未找到匹配实现时抛出 {@link IllegalArgumentException}。
     *
     * @param sourceType 来源类型（如 UPLOAD、URL、TEXT）
     * @return 匹配的文档导入策略
     */
    public DocumentImportStrategy getStrategy(String sourceType) {
        return strategies.stream()
                .filter(s -> s.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported source type: " + sourceType));
    }
}
