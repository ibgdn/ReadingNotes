package com.ibgdn.chapter_3;

import java.nio.ByteBuffer;

/**
 * 直接内存和堆内存申请速度对比
 */
public class AllocDirectBuffer {
    public void directAllocate() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 200000; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1000);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("DirectAllocate: " + (endTime - startTime));
    }

    public void bufferAllocate() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 200000; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("BufferAllocate: " + (endTime - startTime));
    }

    public static void main(String[] args) {
        AllocDirectBuffer allocDirectBuffer = new AllocDirectBuffer();
        allocDirectBuffer.bufferAllocate();
        allocDirectBuffer.directAllocate();

        allocDirectBuffer.bufferAllocate();
        allocDirectBuffer.directAllocate();
    }
}
