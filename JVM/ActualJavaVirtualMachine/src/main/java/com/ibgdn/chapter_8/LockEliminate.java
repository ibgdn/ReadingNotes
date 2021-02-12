package com.ibgdn.chapter_8;

/**
 * StringBuffer 变量的作用域仅限于方法体内部，不可能逃逸出该方法，不会被多个线程同时访问。
 *
 * VM options：
 *
 * -server -XX:+DoEscapeAnalysis -XX:-EliminateLocks -Xcomp -XX:-BackgroundCompilation -XX:BiasedLockingStartupDelay=0
 *
 * -server -XX:+DoEscapeAnalysis -XX:+EliminateLocks -Xcomp -XX:-BackgroundCompilation -XX:BiasedLockingStartupDelay=0
 */
public class LockEliminate {
    private static final int CIRCLE = 2000000;

    public static String createStringBuffer(String s1, String s2) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(s1);
        stringBuffer.append(s2);
        return stringBuffer.toString();
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < CIRCLE; i++) {
            createStringBuffer("JVM", "Diagnosis");
        }
        long bufferCost = System.currentTimeMillis() - start;
        System.out.println("createStringBuffer: " + bufferCost + " ms");
    }
}