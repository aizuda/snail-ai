package com.aizuda.snail.ai.admin.vo.agent;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AgentUsageDetailQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentUsageDetailQueryVO extends BaseQueryVO {

    private String start;

    private String end;
}
