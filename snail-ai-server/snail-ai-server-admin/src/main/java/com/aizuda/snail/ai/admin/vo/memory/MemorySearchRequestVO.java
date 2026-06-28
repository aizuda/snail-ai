package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 记忆搜索请求 VO
 *
 * @author snail-ai
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemorySearchRequestVO {

    /**
     * 搜索查询
     */
    private String query;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 对话 ID（检索同会话加权等，可选）
     */
    private String conversationId;

    /**
     * 嵌入模型 ID（语义检索，可选，默认由服务侧处理）
     */
    private Long embeddingModelId;

    /**
     * 记忆类型过滤
     */
    private List<Integer> types;

    /**
     * 限制数量
     */
    private Integer limit;

    /**
     * 最近天数
     */
    private Integer days;
}
