package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RagChunkResponseVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagChunkResponseVO {

    private Long id;

    private Long ragId;

    private Long documentId;

    private Integer paragraphIndex;

    private Integer chunkIndex;

    private String content;

    private Integer tokenCount;

    private String vectorId;

    private String documentName;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
