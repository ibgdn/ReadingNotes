package com.ibgdn.chapter_2;

/**
 * 方法栈，局部变量表-Local variable table
 * <p>
 * VM options： -Xss128K
 */
public class StackLocalVariableTable {
    private static int count = 0;

    public static void recursion(long longA, long longB, long longC) {
        long longE = 1, longF = 2, longG = 3, longH = 4, longI = 5, longK = 6, longQ = 7, longX = 8, longY = 9, longZ = 10;
        count++;
        recursion(longA, longB, longC);
    }

    public static void main(String[] args) {
        try {
            recursion(0L, 0L, 0L);
        } catch (Throwable t) {
            System.out.println("Deep of calling: " + count);
            t.printStackTrace();
        }
    }
}
