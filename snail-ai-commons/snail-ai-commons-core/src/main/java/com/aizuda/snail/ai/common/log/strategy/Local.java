package com.aizuda.snail.ai.common.log.strategy;

/**
 * (复制自 snail-job)
 *
 * @author opensnail
 * @since 2026-05-05
 */
public final class Local extends AbstractLog {
    public Local() {
        setRemote(Boolean.FALSE);
    }
}
