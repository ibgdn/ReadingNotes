package com.ibgdn.chapter_10;

/**
 * 被动引用
 */
class ParentPR {
    static {
        System.out.println("ParentPR init.");
    }

    public static int v = 100;
}

class ChildPR extends ParentPR {
    static {
        System.out.println("ChildPR init.");
    }
}

public class PassiveReference {
    public static void main(String[] args) {
        System.out.println(ChildPR.v);
    }
}
