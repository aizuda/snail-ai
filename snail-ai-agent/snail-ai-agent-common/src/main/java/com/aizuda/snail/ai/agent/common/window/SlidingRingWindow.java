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
 * @author 唐亚峰 | battcn
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

    /**
     * 以默认环大小（{@value DEFAULT_RING_SIZE}）与独立调度线程池构建窗口
     *
     * @param duration       窗口时长，到期自动 flush
     * @param totalThreshold 数量阈值，窗口内数据量达到后立即 flush
     * @param listener       flush 回调监听器列表
     */
    public SlidingRingWindow(Duration duration, Integer totalThreshold, List<Listener<T>> listener) {
        this(DEFAULT_RING_SIZE, duration, totalThreshold, listener, new ScheduledThreadPoolExecutor(3));
    }

    /**
     * 完整构造：JVM 退出钩子会兜底 flush 所有槽位，避免停机丢数据
     *
     * @param ringSize       环形数组大小（可同时存活的窗口数）
     * @param duration       窗口时长，到期自动 flush
     * @param totalThreshold 数量阈值，窗口内数据量达到后立即 flush
     * @param listener       flush 回调监听器列表
     * @param scheduler      窗口到期调度器（由调用方管理生命周期）
     */
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

    /** 主动关停：flush 所有槽位中未提交的数据（不停止外部传入的调度器） */
    public void shutdown() {
        log.info("Sliding window is about to exit, emitting data in the Window");
        for (int i = 0; i < ringArray.length(); i++) {
            emit(getWindow(i));
        }
    }

    /**
     * 写入一条数据：窗口不存在则创建，已超时则滚动到下一槽位，
     * 在窗口内则入队并按数量阈值/到期标记触发 flush
     *
     * @param data 待写入数据
     */
    public void add(T data) {
        final long now = System.currentTimeMillis();

        // CAS 自旋的三分支状态机：
        //  1) 当前槽为空           -> 创建窗口并写入；
        //  2) 当前窗口已超时        -> 滚动到下一槽（自增 sequencer）创建新窗口；
        //  3) 当前窗口仍在有效期内   -> 直接入队，达到数量阈值或已标记 emit 则立即 flush。
        // CAS 失败（其它线程已抢先创建/滚动）时循环重试，故无需加锁。
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

    /** 注册窗口到期任务：到期后标记 emitted 并 flush（下限 100ms 防止负延迟） */
    private void scheduleWindowEmit(Window<T> window, long now) {
        scheduler.schedule(() -> {
            window.setEmitted();
            emit(window);
        }, Math.max(window.windowTime - now, 100), TimeUnit.MILLISECONDS);
    }

    /** 排空窗口并通知监听器；监听器异常仅记录，不影响窗口滚动与后续数据写入 */
    private void emit(Window<T> window) {
        if (Objects.isNull(window)) {
            return;
        }

        final List<T> drainList = window.drain();
        if (drainList.isEmpty()) {
            return;
        }

        try {
            listener.forEach(consumer -> consumer.handler(drainList));
        } catch (Throwable e) {
            log.error("sliding window emit error", e);
        }
    }

    /** CAS 抢占槽位创建新窗口；被其它线程抢先时返回 null，由调用方自旋重试 */
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
            // WIP(work-in-progress) 计数保证同一时刻只有一个线程真正排空队列：
            // 抢到排空权的线程（wip 从 0 增到 1）循环 poll，直到把期间其它线程
            // 累加的 wip 全部抵消为 0；其余线程仅自增 wip 后立即返回空，避免锁与重复排空。
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
