package com.aizuda.snail.ai.admin.vo.agent;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentQueryVO extends BaseQueryVO {

    /** 搜索关键字 */
    private String keyword;

    /** 是否仅查询企业精选 */
    private Boolean featured;

    /** latest / popular */
    private String sort;
}
