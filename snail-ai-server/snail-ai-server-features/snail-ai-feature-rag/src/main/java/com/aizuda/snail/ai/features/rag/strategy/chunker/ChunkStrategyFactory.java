package com.aizuda.snail.ai.features.rag.strategy.chunker;

import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 切片策略工厂：按 {@link ChunkModeEnum} 选择匹配的 {@link ChunkStrategy} 实现。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class ChunkStrategyFactory {

    private final List<ChunkStrategy> strategies;

    /**
     * 按切片模式选择策略，不支持时抛出 {@link IllegalArgumentException}。
     */
    public ChunkStrategy getStrategy(ChunkModeEnum mode) {
        return strategies.stream()
                .filter(s -> s.supports(mode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的切片模式: " + mode));
    }
}
