package com.aizuda.snail.ai.persistence.rag.mapper;

import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档数据访问层
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface RagDocumentMapper extends BaseMapper<RagDocumentPO> {

    /**
     * 按知识库统计文档数量（用于首页知识库 Top 排行）。
     *
     * @return 每行包含 ragId 与 cnt
     */
    @Select("SELECT rag_id AS ragId, COUNT(*) AS cnt FROM sai_rag_document GROUP BY rag_id")
    List<Map<String, Object>> countDocumentByRag();

    /**
     * 统计指定处理状态的文档数量（用于首页「文档解析失败」告警等）。
     * <p>
     * 状态码由调用方按 RagDocumentStatus 枚举传入，避免在持久层写入魔法值；
     * 在 SQL 内比较可规避 status 为 tinyint(1) 时被 JDBC 映射成 Boolean 的问题。
     *
     * @param status 文档处理状态码（见 RagDocumentStatus）
     * @return 该状态的文档数量
     */
    @Select("SELECT COUNT(*) FROM sai_rag_document WHERE status = #{status}")
    long countByStatus(@Param("status") int status);
}
