package com.aizuda.snail.ai.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页响应
 *
 * @author opensnail
 * @since 2022-02-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class PageResult<T> extends Result<T> {

    /**
     * 当前页码
     */
    private long page;

    /**
     * 每页条数
     */
    private long size;

    /**
     * 总记录数
     */
    private long total;

    public PageResult(int status, String message, T data) {
        super(status, message, data);
    }

    public PageResult() {
        super();
    }
}
