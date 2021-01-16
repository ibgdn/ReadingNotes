package com.ibgdn.chapter_7;

/**
 * 常量引用位置变动
 *
 * 需要添加输入参数
 * 输出：
 * 697960108
 * 943010986
 * 1807837413
 */
public class ConstantPool {
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }

        System.out.println(System.identityHashCode(args[0] + Integer.toString(0)));
        System.out.println(System.identityHashCode((args[0] + Integer.toString(0)).intern()));
        System.gc();
        System.out.println(System.identityHashCode((args[0] + Integer.toString(0)).intern()));
    }
}