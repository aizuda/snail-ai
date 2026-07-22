package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for building searchable image knowledge chunks.
 *
 * @author opensnail
 * @since 2026-07-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageKnowledgeChunkBuildRequest {

    private Long ragId;

    private Long documentId;

    private String documentName;

    private Integer startChunkIndex;

    private List<DoclingParseResult.DoclingImage> images;
}
