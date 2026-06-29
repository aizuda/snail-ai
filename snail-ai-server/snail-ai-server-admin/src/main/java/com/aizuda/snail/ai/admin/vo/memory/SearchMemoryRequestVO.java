package com.aizuda.snail.ai.admin.vo.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆检索请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMemoryRequestVO {

    /** 检索关键字 */
    private String query;
    /** 智能体 ID */
    @NotNull
    private Long agentId;
    /** 用户 ID */
    @NotNull
    private Long userId;
    /** 会话 ID */
    @NotBlank
    private String conversationId;

    /** 返回结果数量上限 */
    @Builder.Default
    private Integer limit = 5;

    /** 是否对召回结果重排序 */
    @Builder.Default
    private Boolean rerank = Boolean.FALSE;
}
