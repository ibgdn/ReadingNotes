package com.ibgdn.chapter_7;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量池内存溢出
 * <p>
 * VM options：
 * -Xmx5m -XX:MaxPermSize=5m
 */
public class StringInternOOM {
    public static void main(String[] args) {
        List<String> stringList = new ArrayList<String>();
        int i = 0;
        while (true) {
            stringList.add(String.valueOf(i++).intern());
        }
    }
}