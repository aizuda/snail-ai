package com.aizuda.snail.ai.admin.security.utils;

import java.util.function.Supplier;

/**
 * SimpleWorkFlow
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

    public SimpleWorkFlow next(SimpleWorkFlow nextWork){
        this.nextWork = nextWork;
        return this;
    }

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
