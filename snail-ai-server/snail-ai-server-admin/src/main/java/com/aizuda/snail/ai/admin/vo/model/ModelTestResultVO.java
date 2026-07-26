package com.aizuda.snail.ai.admin.vo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型连通性验证结果 VO
 *
 * @author opensnail
 * @since 2026-07-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTestResultVO {

    /** 验证是否成功 */
    private boolean success;

    /** 结果消息（成功时显示耗时，失败时显示错误详情） */
    private String message;

    /** 响应耗时（毫秒），失败时为 0 */
    private long responseTimeMs;
}
