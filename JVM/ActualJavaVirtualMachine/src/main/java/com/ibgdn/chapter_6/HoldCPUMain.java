package com.ibgdn.chapter_6;

/**
 * 开启4个线程，1个大量占用 CPU 资源，3个空闲。
 */
public class HoldCPUMain {
    public static class HoldCPUTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                // 持续占用 CPU 资源
                double number = Math.random() * Math.random();
            }
        }
    }

    public static class LazyTask implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    // 休眠线程
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new HoldCPUTask()).start();
        new Thread(new LazyTask()).start();
        new Thread(new LazyTask()).start();
        new Thread(new LazyTask()).start();
    }
}