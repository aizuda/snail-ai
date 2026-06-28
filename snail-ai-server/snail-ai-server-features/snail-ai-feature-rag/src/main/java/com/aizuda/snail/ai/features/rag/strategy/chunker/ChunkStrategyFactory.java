package com.aizuda.snail.ai.features.rag.strategy.chunker;

import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChunkStrategyFactory {

    private final List<ChunkStrategy> strategies;

    public ChunkStrategy getStrategy(ChunkModeEnum mode) {
        return strategies.stream()
                .filter(s -> s.supports(mode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的切片模式: " + mode));
    }
}
