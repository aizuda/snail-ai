package com.aizuda.snail.ai.admin.vo.agent;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AgentQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentQueryVO extends BaseQueryVO {

    private String keyword;

    private Boolean featured;

    /** latest / popular */
    private String sort;
}
