package com.ibgdn.chapter_6;

/**
 * 方法占用 CPU 的时间
 * <p>
 * VM options：
 * -agentlib:hprof=cpu=times,interval=10
 * <p>
 * -agentlib:hprof=heap=dump,format=b,file=D:\core.hprof
 * <p>
 * -agentlib:hprof=heap=sites
 */
public class HProfTest {
    public void slowMethod() {
        try {
            // 模拟一个很慢的方法
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void slowerMethod() {
        try {
            // 模拟一个更慢的方法
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void fastMethod() {
        // 模拟一个很快的方法
        Thread.yield();
    }

    public static void main(String[] args) {
        // 运行所有方法
        HProfTest hProfTest = new HProfTest();
        hProfTest.fastMethod();
        hProfTest.slowMethod();
        hProfTest.slowerMethod();
    }
}