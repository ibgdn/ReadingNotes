package com.ibgdn.chapter_7;

import java.nio.ByteBuffer;

/**
 * 直接内存溢出
 * <p>
 * VM options：
 * -Xmx1m -XX:+PrintGCDetails
 */
public class DirectBufferOOM {
    public static void main(String[] args) {
        for (int i = 0; i < 1024; i++) {
            ByteBuffer.allocateDirect(1024 * 1024 * 10);
            System.out.println("i: " + i);
        }
    }
}