package com.ibgdn.chapter_2;

/**
 * 递归调用栈溢出示例
 * <p>
 * VM options：
 * -Xss128K
 */
public class StackOverflowDeep {
    private static int count = 0;

    public static void recursion() {
        count++;
        recursion();
    }

    public static void main(String[] args) {
        try {
            recursion();
        } catch (Throwable t) {
            System.out.println("Deep of calling: " + count);
            t.printStackTrace();
        }
    }
}
