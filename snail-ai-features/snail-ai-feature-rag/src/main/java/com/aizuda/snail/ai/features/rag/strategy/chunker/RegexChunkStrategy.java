package com.aizuda.snail.ai.features.rag.strategy.chunker;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 按 Java 正则一级切分，再递归按长度切分。正则为空时回退为 \\n\\n 分隔。
 */
@Slf4j
@Component
public class RegexChunkStrategy extends AbstractChunkStrategy {

    public RegexChunkStrategy(TokenAwareChunker chunker) {
        super(chunker);
    }

    @Override
    public boolean supports(ChunkModeEnum mode) {
        return ChunkModeEnum.REGEX == mode;
    }

    @Override
    protected String[] splitIntoParagraphs(ChunkContext ctx) {
        String regex = StrUtil.trimToNull(ctx.getChunkRegex());
        if (regex == null) {
            log.warn("regex mode but pattern is empty, fallback to \\n\\n");
            List<String> delimiters = chunker.resolveDelimiterList("\n\n");
            return chunker.splitByAnyDelimiter(ctx.getContent(), delimiters);
        }
        final Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("无效的正则表达式: " + e.getMessage(), e);
        }
        return pattern.split(ctx.getContent(), -1);
    }
}
