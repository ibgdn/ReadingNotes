package com.ibgdn.chapter_3;

/**
 * 堆空间大小
 * <p>
 * VM options：
 * -Xmx20m -Xms5m -XX:+PrintCommandLineFlags -XX:+PrintGCDetails -XX:+UseSerialGC
 */
public class HeapAlloc {
    public static void main(String[] args) {
        printMemory();

        byte[] bytes = new byte[1 * 1024 * 1024];
        System.out.println("分配了 1M 空间给数组 bytes");

        printMemory();

        bytes = new byte[4 * 1024 * 1024];
        System.out.println("分配了 4M 空间给数组 bytes");

        printMemory();
    }

    private static void printMemory() {
        System.out.print("Max  Memory : ");
        System.out.println(Runtime.getRuntime().maxMemory() + " bytes");
        System.out.print("Free  Memory: ");
        System.out.println(Runtime.getRuntime().freeMemory() + " bytes");
        System.out.print("Total Memory: ");
        System.out.println(Runtime.getRuntime().totalMemory() + " bytes");
        System.out.println();
    }
}
