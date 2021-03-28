package com.ibgdn.chapter_10;

/**
 * 主动引用
 */
public class Parent {
    static {
        System.out.println("Parent init.");
    }
}

class Child extends Parent {
    static {
        System.out.println("Child init.");
    }
}

class initMain {
    public static void main(String[] args) {
        Child child = new Child();
    }
}