package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.features.rag.enums.DocumentParseEngineEnum;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
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
     *
     * @param fileType 文件类型（如 txt、md、pdf）
     * @return 匹配的文档解析器
     */
    public DocumentParser getParser(String fileType) {
        return parsers.stream()
                .filter(p -> p.supports(fileType))
                .filter(p -> DocumentParseEngineEnum.DEFAULT.getValue().equals(p.parserType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + fileType));
    }

    public DocumentParser getParser(DocumentParseRequest request) {
        String fileType = request == null ? null : request.getFileType();
        DocumentParseEngineEnum engine = resolveEngine(request == null ? null : request.getConfig());
        if (engine == DocumentParseEngineEnum.DOCLING) {
            // 只有知识库显式选择 docling 且 DoclingParser 当前支持该文件类型时，才走外部 Docling 服务。
            return parsers.stream()
                    .filter(p -> DocumentParseEngineEnum.DOCLING.getValue().equalsIgnoreCase(p.parserType()))
                    .filter(p -> p.supports(fileType))
                    .findFirst()
                    .orElseGet(() -> getFallbackParser(fileType));
        }
        return getFallbackParser(fileType);
    }

    public DocumentParser getFallbackParser(String fileType) {
        // 回退解析器必须排除 Docling，避免 Docling 服务不可用时再次选回同一个解析器。
        return getParser(fileType);
    }

    private DocumentParseEngineEnum resolveEngine(RagConfigDO config) {
        if (config == null || config.getParseParams() == null) {
            return DocumentParseEngineEnum.DEFAULT;
        }
        return DocumentParseEngineEnum.fromValue(config.getParseParams().getEngine());
    }
}
