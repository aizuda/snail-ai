package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MemoryStatsQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryStatsQueryVO extends BaseQueryVO {
    private Integer days = 30;
}
