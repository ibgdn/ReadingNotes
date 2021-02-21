package com.ibgdn.chapter_8;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

/**
 * LongAddr 累加计数
 */
public class LongAdderCount {
    // 线程数
    private static final int MAX_THREADS = 3;
    // 任务数
    private static final int TASK_COUNT = 3;
    // 目标总数
    private static final int TARGET_COUNT = 10000000;

    private LongAdder longAdderCount = new LongAdder();
    static CountDownLatch countDownLatch = new CountDownLatch(TASK_COUNT);

    public class LongAdderThread implements Runnable {
        protected String name;
        protected long startTime;

        public LongAdderThread(long startTime) {
            this.startTime = startTime;
        }

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            long v = longAdderCount.sum();
            while (v < TARGET_COUNT) {
                longAdderCount.increment();
                v = longAdderCount.sum();
            }
            long endTime = System.currentTimeMillis();
            System.out.println("LongAdder spend: " + (endTime - startTime) + "ms v = " + v);
            countDownLatch.countDown();
        }
    }

    @Test
    public void testAtomicLong() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
        long startTime = System.currentTimeMillis();
        LongAdderThread atomic = new LongAdderThread(startTime);
        for (int i = 0; i < TASK_COUNT; i++) {
            executorService.submit(atomic);
        }
        countDownLatch.await();
        executorService.shutdown();
    }
}