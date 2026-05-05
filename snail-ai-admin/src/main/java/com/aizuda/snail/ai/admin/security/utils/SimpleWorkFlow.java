package com.aizuda.snail.ai.admin.security.utils;

import java.util.function.Supplier;

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
        if (work.get() == Boolean.TRUE) {
            if (nextWork == null){
                return true;
            }
            return nextWork.start();
        }
        return false;
    }
}
