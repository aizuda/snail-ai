package com.aizuda.snail.ai.admin.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: opensnail
 * @date : 2022-02-16 14:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends com.aizuda.snail.ai.common.model.PageResult<T> {

    public PageResult(int status, String message, T data) {
        super(status, message, data);
    }

    public PageResult() {
        super();
    }

    public PageResult(PageDTO pageDTO, T data) {
        super();
        setPage(pageDTO.getCurrent());
        setSize(pageDTO.getSize());
        setTotal(pageDTO.getTotal());
        setData(data);
    }

    public PageResult(Page page, T data) {
        super();
        setPage(page.getCurrent());
        setSize(page.getSize());
        setTotal(page.getTotal());
        setData(data);
    }
}
