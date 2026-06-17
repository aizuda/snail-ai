package com.aizuda.snail.ai.persistence.rag.mapper;

import java.util.List;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface RagChunkMapper extends BaseMapper<RagChunkPO> {

    List<SearchResult> fullTextSearch(@Param("tsQuery") String tsQuery,
                                      @Param("ragId") Long ragId,
                                      @Param("limit") int limit);

    Integer selectMaxChunkIndex(@Param("documentId") Long documentId);
}
