package com.aizuda.snail.ai.admin.vo;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StoreInstanceQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StoreInstanceQueryVO extends BaseQueryVO {

    private Integer category;
}
