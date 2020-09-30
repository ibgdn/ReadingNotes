package com.ibgdn.chapter_2;

/**
 * 设置 java 虚拟机简单参数。
 */
public class SimpleArgs {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.println("参数_" + (i + 1) + " : " + args[i]);
        }
        System.out.println("-Xmx: " + Runtime.getRuntime().maxMemory() / 1000 / 1000 + " M.");
    }
}
