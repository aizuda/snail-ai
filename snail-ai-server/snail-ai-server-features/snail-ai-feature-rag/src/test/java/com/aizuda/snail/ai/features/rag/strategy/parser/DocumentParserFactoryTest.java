package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.features.rag.enums.DocumentParseEngineEnum;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class DocumentParserFactoryTest {

    @Test
    void getParserUsesDoclingParserWhenConfigured() {
        DocumentParser defaultParser = new FakeParser("default");
        DocumentParser doclingParser = new FakeParser(DocumentParseEngineEnum.DOCLING.getValue());
        DocumentParserFactory factory = new DocumentParserFactory(List.of(defaultParser, doclingParser));

        RagConfigDO config = new RagConfigDO();
        config.setParseParams(RagConfigDO.ParseParams.builder()
                .engine(DocumentParseEngineEnum.DOCLING.getValue())
                .build());
        DocumentParseRequest request = DocumentParseRequest.builder()
                .fileType("pdf")
                .config(config)
                .build();

        assertSame(doclingParser, factory.getParser(request));
    }

    @Test
    void getFallbackParserIgnoresDoclingParser() {
        DocumentParser defaultParser = new FakeParser("default");
        DocumentParser doclingParser = new FakeParser(DocumentParseEngineEnum.DOCLING.getValue());
        DocumentParserFactory factory = new DocumentParserFactory(List.of(doclingParser, defaultParser));

        assertSame(defaultParser, factory.getFallbackParser("pdf"));
    }

    private static class FakeParser implements DocumentParser {
        private final String parserType;

        private FakeParser(String parserType) {
            this.parserType = parserType;
        }

        @Override
        public boolean supports(String fileType) {
            return "pdf".equalsIgnoreCase(fileType);
        }

        @Override
        public String parserType() {
            return parserType;
        }

        @Override
        public String parse(InputStream inputStream) {
            return parserType;
        }
    }
}
