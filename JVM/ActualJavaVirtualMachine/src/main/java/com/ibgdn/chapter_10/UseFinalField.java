package com.ibgdn.chapter_10;

/**
 * 引用 final 常量并不会引起类的初始化
 */
class FinalFieldClass {
    public static final String constString = "CONST";

    static {
        System.out.println("FinalFieldClass init.");
    }
}

public class UseFinalField {
    public static void main(String[] args) {
        System.out.println(FinalFieldClass.constString);
    }
}