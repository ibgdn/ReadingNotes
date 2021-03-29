package com.ibgdn.chapter_10;

/**
 * 主动引用
 */
class ParentAR {
    static {
        System.out.println("ParentAR init.");
    }
}

class ChildAR extends ParentPR {
    static {
        System.out.println("ChildAR init.");
    }
}

class initMain {
    public static void main(String[] args) {
        ChildAR child = new ChildAR();
    }
}