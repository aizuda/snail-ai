package com.aizuda.snail.ai.admin.security.utils;

import java.util.function.Supplier;

/**
 * 简单工作流，将多个布尔步骤串成链式执行，任一步骤返回 false 即中断并整体失败。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public class SimpleWorkFlow {

    private Supplier<Boolean> work;

    private SimpleWorkFlow nextWork;

    public SimpleWorkFlow(Supplier<Boolean> work){
        this.work = work;
    }

    /**
     * 设置下一个步骤
     *
     * @param nextWork 下一个工作流节点
     * @return 当前节点（便于链式调用）
     */
    public SimpleWorkFlow next(SimpleWorkFlow nextWork){
        this.nextWork = nextWork;
        return this;
    }

    /**
     * 依次执行各步骤，全部成功返回 {@code true}，任一步骤失败返回 {@code false}
     *
     * @return 整体是否成功
     */
    public boolean start(){
        if (Boolean.TRUE.equals(work.get())) {
            if (nextWork == null){
                return true;
            }
            return nextWork.start();
        }
        return false;
    }
}
