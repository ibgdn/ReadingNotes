package com.ibgdn.chapter_5;

import java.util.HashMap;
import java.util.Map;

/**
 * 对象进入老年代
 * <p>
 * VM options：
 * -Xmx1024M -Xms1024M -XX:+PrintGCDetails -XX:MaxTenuringThreshold=15 -XX:+PrintHeapAtGC
 */
public class MaxTenuringThreshold {
    public static final int _1M = 1024 * 1024;
    public static final int _1K = 1024;

    public static void main(String[] args) {
        Map<Integer, byte[]> map = new HashMap<>();
        for (int i = 0; i < 5 * _1K; i++) {
            // 将生成的 byte 数组进行保存，防止被 GC 回收
            byte[] bytes = new byte[_1K];
            map.put(i, bytes);
        }
        // 在新生代分配内存空间，以触发新生代 GC
        for (int i = 0; i < 17; i++) {
            for (int j = 0; j < 270; j++) {
                byte[] bytes = new byte[_1M];
            }
        }
    }
}