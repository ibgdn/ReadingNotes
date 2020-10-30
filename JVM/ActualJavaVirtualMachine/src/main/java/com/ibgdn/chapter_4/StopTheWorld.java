package com.ibgdn.chapter_4;

import java.util.HashMap;
import java.util.Map;

/**
 * Stop The World.
 * <p>
 * VM options：
 * -Xmx1g -Xms1g -Xmn512k -XX:+UseSerialGC -Xloggc:gc.log -XX:+PrintGCDetails
 * -Xmx1g -Xms1g -Xmn900m -XX:SurvivorRatio=1 -XX:+UseSerialGC -Xloggc:gc.log -XX:+PrintGCDetails
 */
public class StopTheWorld {
    public static class MyThread extends Thread {
        Map map = new HashMap();

        /**
         * If this thread was constructed using a separate
         * <code>Runnable</code> run object, then that
         * <code>Runnable</code> object's <code>run</code> method is called;
         * otherwise, this method does nothing and returns.
         * <p>
         * Subclasses of <code>Thread</code> should override this method.
         *
         * @see #start()
         * @see #stop()
         * @see #Thread(ThreadGroup, Runnable, String)
         */
        @Override
        public void run() {
            try {
                while (true) {
                    // -Xmx1g -Xms1g -Xmn512k -XX:+UseSerialGC -Xloggc:gc.log -XX:+PrintGCDetails
//                  if (map.size() * 512 / 1024 / 1024 >= 900) {
                    // -Xmx1g -Xms1g -Xmn900m -XX:SurvivorRatio=1 -XX:+UseSerialGC -Xloggc:gc.log -XX:+PrintGCDetails
                    if (map.size() * 512 / 1024 / 1024 >= 550) {
                        map.clear();
                        System.out.println("Clean map");
                    }
                    byte[] bytes;
                    for (int i = 0; i < 100; i++) {
                        bytes = new byte[512];
                        map.put(System.nanoTime(), bytes);
                    }
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PrintThread extends Thread {
        public static final long startTime = System.currentTimeMillis();

        /**
         * If this thread was constructed using a separate
         * <code>Runnable</code> run object, then that
         * <code>Runnable</code> object's <code>run</code> method is called;
         * otherwise, this method does nothing and returns.
         * <p>
         * Subclasses of <code>Thread</code> should override this method.
         *
         * @see #start()
         * @see #stop()
         * @see #Thread(ThreadGroup, Runnable, String)
         */
        @Override
        public void run() {
            try {
                while (true) {
                    long time = System.currentTimeMillis() - startTime;
                    System.out.println(time / 1000 + "." + time % 1000);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        MyThread myThread = new MyThread();
        PrintThread printThread = new PrintThread();
        myThread.start();
        printThread.start();
    }
}
