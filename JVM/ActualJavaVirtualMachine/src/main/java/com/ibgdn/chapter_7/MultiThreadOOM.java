package com.ibgdn.chapter_7;

/**
 * 多线程内存溢出
 * <p>
 * VM options：
 * -Xmx1m
 */
public class MultiThreadOOM {
    public static class SleepThread implements Runnable {

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
                Thread.sleep(10000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1500; i++) {
            new Thread(new SleepThread(), " Thread " + i).start();
            System.out.println("Thread " + i + " created.");
        }
    }
}