package com.ibgdn.chapter_8;

import java.util.List;
import java.util.Vector;

/**
 * 偏向锁
 *
 * VM options：
 * -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 -client -Xmx512m -Xms512m
 */
public class Biased {
    public static List<Integer> numberList = new Vector<>();

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        int count = 0;
        int startNum = 0;
        while (count < 10000000) {
            numberList.add(startNum);
            startNum += 2;
            count++;
        }
        long end = System.currentTimeMillis();
        System.out.println(end - begin);
    }
}