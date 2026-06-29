package com.aizuda.snail.ai.admin.vo;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 存储实例分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StoreInstanceQueryVO extends BaseQueryVO {

    /** 实例分类 */
    private Integer category;
}
