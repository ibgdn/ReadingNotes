package com.ibgdn.chapter_2;

/**
 * 栈上逃逸分析
 * <p>
 * VM options：
 * -server -Xmx10m -Xms10m -XX:+DoEscapeAnalysis -XX:+PrintGC -XX:-UseTLAB -XX:-EliminateAllocations
 */
public class StackEscapeAnalysis {
    public static class User {
        public int id = 0;
        public String name = "";
    }

    public static void alloc() {
        User user = new User();
        user.id = 5;
        user.name = "stack";
    }

    public static void main(String[] args) {
        long timeMillisStart = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            alloc();
        }
        long timeMillisEnd = System.currentTimeMillis();
        System.out.println("100000000 个 User 对象花费时间：" + (timeMillisEnd - timeMillisStart));
    }
}