package com.ibgdn.chapter_3;

import java.nio.ByteBuffer;

/**
 * 直接内存和堆内存的读写对比
 * <p>
 * VM options：
 * -server
 */
public class AccessDirectBuffer {
    /**
     * 直接内存读写
     */
    public void directAccess() {
        long startTime = System.currentTimeMillis();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(500);
        for (int i = 0; i < 100000; i++) {
            for (int j = 0; j < 99; j++) {
                byteBuffer.putInt(j);
            }
            byteBuffer.flip();
            for (int j = 0; j < 99; j++) {
                byteBuffer.getInt();
            }
            byteBuffer.clear();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("TestDirectWrite: " + (endTime - startTime));
    }

    /**
     * 堆内存读写
     */
    public void bufferAccess() {
        long startTime = System.currentTimeMillis();
        ByteBuffer byteBuffer = ByteBuffer.allocate(500);
        for (int i = 0; i < 100000; i++) {
            for (int j = 0; j < 99; j++) {
                byteBuffer.putInt(j);
            }
            byteBuffer.flip();
            for (int j = 0; j < 99; j++) {
                byteBuffer.getInt();
            }
            byteBuffer.clear();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("TestBufferWrite: " + (endTime - startTime));
    }

    public static void main(String[] args) {
        AccessDirectBuffer accessDirectBuffer = new AccessDirectBuffer();
        accessDirectBuffer.bufferAccess();
        accessDirectBuffer.directAccess();

        accessDirectBuffer.bufferAccess();
        accessDirectBuffer.directAccess();
    }
}
