package com.aizuda.snail.ai.common.log.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简化版 AbstractLog，使用 SLF4J (复制自 snail-job 并简化)
 */
public abstract class AbstractLog {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLog.class);
    private Boolean isRemote = Boolean.FALSE;

    public Boolean getRemote() {
        return isRemote;
    }

    protected void setRemote(final Boolean remote) {
        isRemote = remote;
    }

    public void trace(String format, Object... arguments) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(format, arguments);
        }
    }

    public void debug(String format, Object... arguments) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(format, arguments);
        }
    }

    public void info(String format, Object... arguments) {
        LOG.info(format, arguments);
    }

    public void warn(String format, Object... arguments) {
        LOG.warn(format, arguments);
    }

    public void error(Throwable e) {
        LOG.error("", e);
    }

    public void error(String format, Object... arguments) {
        LOG.error(format, arguments);
    }
}
