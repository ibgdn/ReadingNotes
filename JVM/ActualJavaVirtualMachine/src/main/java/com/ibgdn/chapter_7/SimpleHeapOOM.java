package com.ibgdn.chapter_7;

import java.util.ArrayList;

/**
 * 堆内存溢出
 * <p>
 * VM options：
 * -Xmx1m
 */
public class SimpleHeapOOM {
    public static void main(String[] args) {
        ArrayList<byte[]> bytes = new ArrayList<>();
        for (int i = 0; i < 1024; i++) {
            bytes.add(new byte[1024 * 1024]);
        }
    }
}
