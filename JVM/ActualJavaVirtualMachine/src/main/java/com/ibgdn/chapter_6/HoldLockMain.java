package com.ibgdn.chapter_6;

import java.util.Random;

/**
 * vmstat 工具
 */
public class HoldLockMain {
    public static Object[] lockObject = new Object[10];

    public static Random random = new Random();

    static {
        for (int i = 0; i < lockObject.length; i++) {
            lockObject[i] = new Object();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < lockObject.length * 2; i++) {
            // 每2个线程使用同一个对象
            new Thread(new HoldLockTask(i / 2)).start();
        }
    }

    /**
     * 一个持有锁的线程
     */
    public static class HoldLockTask implements Runnable {

        private int i;

        public HoldLockTask(int i) {
            this.i = i;
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
            try {
                while (true) {
                    // 持有锁
                    synchronized (lockObject[i]) {
                        if (i % 2 == 0) {
                            // 等待
                            lockObject[i].wait(random.nextInt(10));
                        } else {
                            // 通知
                            lockObject[i].notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}