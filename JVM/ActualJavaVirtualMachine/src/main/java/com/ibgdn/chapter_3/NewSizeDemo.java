package com.ibgdn.chapter_3;

/**
 * 新生代配置
 * <p>
 * VM options：
 * -Xmx20m -Xms20m -Xmn1m -XX:SurvivorRatio=2 -XX:+PrintGCDetails
 */
public class NewSizeDemo {
    public static void main(String[] args) {
        byte[] bytes = null;
        for (int i = 0; i < 10; i++) {
            bytes = new byte[1 * 1024 * 1024];
        }
    }
}
