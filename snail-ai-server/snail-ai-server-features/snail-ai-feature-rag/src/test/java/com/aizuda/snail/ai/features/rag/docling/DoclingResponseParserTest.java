package com.aizuda.snail.ai.features.rag.docling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoclingResponseParserTest {

    private final DoclingResponseParser parser = new DoclingResponseParser();

    @Test
    void parseResultExtractsMarkdownElementsTablesImagesAndMetadata() {
        String json = """
                {
                  "document": {
                    "md_content": "# Report\\n\\nBody",
                    "json_content": {
                      "pages": {
                        "1": {
                          "image": {
                            "uri": "data:image/png;base64,page-image"
                          }
                        }
                      },
                      "texts": [
                        {
                          "label": "section_header",
                          "text": "Report",
                          "level": 1,
                          "prov": [{"page_no": 1}]
                        },
                        {
                          "label": "text",
                          "text": "Body",
                          "prov": [{"page_no": 1}]
                        }
                      ],
                      "tables": [
                        {
                          "prov": [{"page_no": 1}],
                          "data": {
                            "num_rows": 2,
                            "num_cols": 2,
                            "table_cells": [
                              {"start_row_offset_idx": 0, "start_col_offset_idx": 0, "text": "A"},
                              {"start_row_offset_idx": 0, "start_col_offset_idx": 1, "text": "B"},
                              {"start_row_offset_idx": 1, "start_col_offset_idx": 0, "text": "1"},
                              {"start_row_offset_idx": 1, "start_col_offset_idx": 1, "text": "2"}
                            ]
                          }
                        }
                      ],
                      "pictures": [
                        {
                          "prov": [{"page_no": 1}],
                          "captions": [{"text": "Architecture"}],
                          "image": {
                            "uri": "data:image/png;base64,picture-image"
                          }
                        }
                      ],
                      "body": {
                        "children": [
                          {"$ref": "#/texts/0"},
                          {"$ref": "#/texts/1"},
                          {"$ref": "#/tables/0"},
                          {"$ref": "#/pictures/0"}
                        ]
                      },
                      "origin": {
                        "filename": "report.pdf"
                      }
                    }
                  }
                }
                """;

        DoclingParseResult result = parser.parse(json, 42L);

        assertTrue(result.isSuccess());
        assertEquals("# Report\n\nBody", result.getMarkdownContent());
        assertEquals(1, result.getPageCount());
        assertEquals(4, result.getElementCount());
        assertEquals(1, result.getTableCount());
        assertEquals(1, result.getImageCount());
        assertEquals(42, result.getParseTime());
        assertEquals("title", result.getElements().get(0).getType());
        assertEquals("| A | B |\n|---|---|\n| 1 | 2 |", result.getElements().get(2).getText());
        assertEquals("picture-image", result.getImages().get(0).getBase64());
        assertEquals("Architecture", result.getImages().get(0).getCaption());
        assertTrue(result.getDocMetadata().contains("report.pdf"));
    }

    @Test
    void parseResultFailsWhenDocumentFieldIsMissing() {
        DoclingParseResult result = parser.parse("{}", 10L);

        assertFalse(result.isSuccess());
        assertEquals("Docling response missing document field", result.getErrorMessage());
    }

    @Test
    void parseResultFailsWhenJsonIsMalformed() {
        DoclingParseResult result = parser.parse("{", 10L);

        assertFalse(result.isSuccess());
        assertEquals("Docling response json is invalid", result.getErrorMessage());
    }

    @Test
    void parseResultNormalizesWhitespaceInImageBase64() {
        String json = """
                {
                  "document": {
                    "json_content": {
                      "pictures": [
                        {
                          "prov": [{"page_no": 1}],
                          "image": {
                            "uri": "data:image/png;base64,Zm9v\\nYmFy"
                          }
                        }
                      ],
                      "body": {
                        "children": [
                          {"$ref": "#/pictures/0"}
                        ]
                      }
                    }
                  }
                }
                """;

        DoclingParseResult result = parser.parse(json, 10L);

        assertTrue(result.isSuccess());
        assertEquals("Zm9vYmFy", result.getImages().get(0).getBase64());
    }
}
