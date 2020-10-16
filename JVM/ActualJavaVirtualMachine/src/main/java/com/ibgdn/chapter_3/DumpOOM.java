package com.ibgdn.chapter_3;


import java.util.Vector;

/**
 * 堆内存溢出信息记录
 * <p>
 * VM options：
 * -Xmx20m -Xms5m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:/oom.dump
 */
public class DumpOOM {
    public static void main(String[] args) {
        Vector<Object> objects = new Vector<>();
        for (int i = 0; i < 25; i++) {
            objects.add(new byte[1 * 1024 * 1024]);
        }
    }
}
