package com.ibgdn.chapter_5;

/**
 * 初创对象在 Eden 区
 * <p>
 * VM options：
 * -Xmx64M -Xms64M -XX:+PrintGCDetails
 */
public class AllocEden {
    public static final int _1K = 1024;

    public static void main(String[] args) {
        for (int i = 0; i < 5 * _1K; i++) {
            byte[] bytes = new byte[_1K];
        }
    }
}