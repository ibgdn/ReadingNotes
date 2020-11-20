package com.ibgdn.chapter_5;

import java.util.HashMap;
import java.util.Map;

/**
 * 大对象进入老年代
 * <p>
 * VM options：
 * -Xmx32m -Xms32m -XX:+UseSerialGC -XX:+PrintGCDetails
 * -Xmx32m -Xms32m -XX:+UseSerialGC -XX:+PrintGCDetails -XX:PretenureSizeThreshold=1000
 * -Xmx32m -Xms32m -XX:+UseSerialGC -XX:+PrintGCDetails -XX:-UseTLAB -XX:PretenureSizeThreshold=1000
 */
public class PretenureSizeThreshold {
    public static final int _1K = 1024;

    public static void main(String[] args) {
        // 5MB空间，6000个 byte 数组，每个数组大小 1024。
        Map<Integer, byte[]> map = new HashMap<>();
        for (int i = 0; i < 5 * _1K; i++) {
            byte[] bytes = new byte[_1K];
            map.put(i, bytes);
        }
    }
}