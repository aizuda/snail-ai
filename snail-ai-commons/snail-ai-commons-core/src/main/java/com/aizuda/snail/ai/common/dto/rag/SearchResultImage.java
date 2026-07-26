package com.aizuda.snail.ai.common.dto.rag;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Image evidence associated with a RAG search result.
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResultImage {

    private Long id;

    private Long ragId;

    private Long documentId;

    private Long chunkId;

    private Long resourceId;

    private Integer imageIndex;

    private String imageUrl;

    private String caption;

    private String figureNo;

    private String figureTitle;

    private String sectionTitle;

    private Integer sourcePage;

    private String documentName;

    private String ocrText;

    private String matchType;

    private Integer matchScore;

    private String matchReason;

    private List<String> matchedTerms;

    private List<String> matchedFields;
}
