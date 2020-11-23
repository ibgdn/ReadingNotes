package com.ibgdn.chapter_5;

/**
 * 一个糟糕的 finalizer() 实现
 * <p>
 * VM options：
 * -Xmx10m -Xms10m -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="D:/oom-5.dump"
 */
public class LongFinalize {
    public static class LF {
        private byte[] content = new byte[512];

        @Override
        protected void finalize() {
            System.out.println(Thread.currentThread().getId());
            try {
                // 模拟一个耗时操作
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        for (int i = 0; i < 50000; i++) {
            LF lf = new LF();
        }
        long end = System.currentTimeMillis();
        System.out.println("The time spent is " + (end - start));
    }
}