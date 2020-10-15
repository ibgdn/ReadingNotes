package com.ibgdn.chapter_3;

/**
 * 新生代配置
 * <p>
 * VM options：
 * -Xmx20m -Xms20m -Xmn1m -XX:SurvivorRatio=2 -XX:+PrintGCDetails
 * -Xmx20m -Xms20m -Xmn7m -XX:SurvivorRatio=2 -XX:+PrintGCDetails
 * -Xmx20m -Xms20m -Xmn16m -XX:SurvivorRatio=8 -XX:+PrintGCDetails
 * -Xmx20m -Xms20m -XX:NewRatio=2 -XX:+PrintGCDetails
 */
public class NewSizeDemo {
    public static void main(String[] args) {
        byte[] bytes = null;
        for (int i = 0; i < 10; i++) {
            bytes = new byte[1 * 1024 * 1024];
        }
    }
}
