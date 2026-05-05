package com.aizuda.snail.ai.agent.common.window;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

/**
 * 环形滑动窗口
 * <p>
 * 支持时间触发和数量触发两种 flush 模式，线程安全，CAS 无锁设计。
 *
 * @author opensnail
 */
@Slf4j
public class SlidingRingWindow<T> {

    private final AtomicReferenceArray<Window<T>> ringArray;
    private final Duration duration;
    private final Integer totalThreshold;
    private final List<Listener<T>> listener;
    private final AtomicLong sequencer = new AtomicLong();
    private final ScheduledExecutorService scheduler;

    private static final int DEFAULT_RING_SIZE = 4;

    public SlidingRingWindow(Duration duration, Integer totalThreshold, List<Listener<T>> listener) {
        this(DEFAULT_RING_SIZE, duration, totalThreshold, listener, new ScheduledThreadPoolExecutor(3));
    }

    public SlidingRingWindow(int ringSize, Duration duration, Integer totalThreshold,
                             List<Listener<T>> listener, ScheduledExecutorService scheduler) {
        this.duration = duration;
        this.totalThreshold = totalThreshold;
        this.ringArray = new AtomicReferenceArray<>(ringSize);
        this.listener = listener;
        this.scheduler = scheduler;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM is about to exit, emitting data in the Window");
            for (int i = 0; i < ringSize; i++) {
                emit(getWindow(i));
            }
        }));
    }

    public void shutdown() {
        log.info("Sliding window is about to exit, emitting data in the Window");
        for (int i = 0; i < ringArray.length(); i++) {
            emit(getWindow(i));
        }
    }

    public void add(T data) {
        final long now = System.currentTimeMillis();

        for (; ; ) {
            final long currentSequence = sequencer.get();
            final Window<T> currentWindow = getWindow(currentSequence);

            if (currentWindow == null) {
                Window<T> newWindow;
                if ((newWindow = createNewWindow(currentSequence, null, now)) != null) {
                    newWindow.getQueue().add(data);
                    scheduleWindowEmit(newWindow, now);
                    return;
                }
            } else if (currentWindow.isOutWindow(now)) {
                Window<T> newWindow;
                final long nextSequence = currentSequence + 1;
                final Window<T> nextWindow = getWindow(nextSequence);
                if ((nextWindow == null || nextWindow.isOutWindow(now))
                        && (newWindow = createNewWindow(nextSequence, nextWindow, now)) != null) {
                    sequencer.incrementAndGet();
                    newWindow.getQueue().add(data);
                    scheduleWindowEmit(newWindow, now);
                    return;
                }
            } else {
                final ConcurrentLinkedQueue<T> queue = currentWindow.getQueue();
                queue.add(data);
                if (queue.size() >= totalThreshold || currentWindow.getEmitStatus()) {
                    emit(currentWindow);
                }
                return;
            }
        }
    }

    private void scheduleWindowEmit(Window<T> window, long now) {
        scheduler.schedule(() -> {
            window.setEmitted();
            emit(window);
        }, Math.max(window.windowTime - now, 100), TimeUnit.MILLISECONDS);
    }

    private void emit(Window<T> window) {
        if (Objects.isNull(window)) return;

        final List<T> drainList = window.drain();
        if (drainList.isEmpty()) return;

        try {
            listener.forEach(consumer -> consumer.handler(drainList));
        } catch (Throwable e) {
            log.error("sliding window emit error", e);
        }
    }

    private Window<T> createNewWindow(long sequence, Window<T> oldWindow, long now) {
        final var index = getIndex(sequence);
        Window<T> newWindow = new Window<>(now + duration.toMillis(), ConcurrentLinkedQueue::new);
        if (ringArray.compareAndSet(index, oldWindow, newWindow)) {
            return newWindow;
        }
        return null;
    }

    private Window<T> getWindow(long sequence) {
        return ringArray.get(getIndex(sequence));
    }

    private int getIndex(long sequence) {
        return (int) (sequence % ringArray.length());
    }

    private static final class Window<T> {
        private final long windowTime;
        private final AtomicBoolean emitStatus = new AtomicBoolean(false);
        private final Supplier<ConcurrentLinkedQueue<T>> dataQueueSupplier;
        private volatile ConcurrentLinkedQueue<T> dataQueue;
        private final AtomicInteger wip = new AtomicInteger();

        private Window(long windowTime, Supplier<ConcurrentLinkedQueue<T>> dataQueueSupplier) {
            this.windowTime = windowTime;
            this.dataQueueSupplier = dataQueueSupplier;
        }

        public void setEmitted() { emitStatus.set(true); }
        public boolean getEmitStatus() { return emitStatus.get(); }
        public boolean isOutWindow(long now) { return windowTime < now; }

        public ConcurrentLinkedQueue<T> getQueue() {
            if (dataQueue == null) {
                synchronized (this) {
                    if (dataQueue == null) {
                        dataQueue = dataQueueSupplier.get();
                    }
                }
            }
            return dataQueue;
        }

        public List<T> drain() {
            if (wip.getAndAdd(1) == 0) {
                final List<T> list = new ArrayList<>();
                var missed = -1;
                do {
                    T element;
                    while ((element = dataQueue.poll()) != null) {
                        list.add(element);
                    }
                } while ((missed = wip.addAndGet(-missed)) != 0);
                return list;
            }
            return List.of();
        }
    }
}
