package com.aizuda.snail.ai.admin.vo.knowledge;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeQueryVO extends BaseQueryVO {

    /** 知识库名称（模糊查询） */
    private String name;
}
