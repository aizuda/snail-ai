package com.aizuda.snail.ai.persistence.rag.mapper;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RagChunkMapper extends BaseMapper<RagChunkPO> {

    @Select("""
            SELECT id AS chunkId, content,
                   ts_rank(to_tsvector('simple', content_segment),
                           to_tsquery('simple', #{tsQuery})) AS score
            FROM snail_ai_rag_chunk
            WHERE rag_id = #{ragId}
              AND content_segment IS NOT NULL
              AND to_tsvector('simple', content_segment) @@ to_tsquery('simple', #{tsQuery})
            ORDER BY score DESC
            LIMIT #{limit}
            """)
    List<SearchResult> fullTextSearch(@Param("tsQuery") String tsQuery,
                                      @Param("ragId") Long ragId,
                                      @Param("limit") int limit);

    @Select("SELECT MAX(chunk_index) FROM snail_ai_rag_chunk WHERE document_id = #{documentId}")
    Integer selectMaxChunkIndex(@Param("documentId") Long documentId);
}
