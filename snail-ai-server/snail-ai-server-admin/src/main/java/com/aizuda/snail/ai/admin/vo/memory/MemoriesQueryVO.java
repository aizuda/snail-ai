package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 记忆分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MemoriesQueryVO extends BaseQueryVO {

    /** 智能体 ID */
    private Long agentId;
    /** 用户 ID */
    private Long userId;
    /** 会话 ID */
    private String conversationId;
    /** 记忆状态 */
    private Integer status;

}
