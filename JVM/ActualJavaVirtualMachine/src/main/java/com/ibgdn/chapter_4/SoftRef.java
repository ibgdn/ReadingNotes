package com.ibgdn.chapter_4;

import java.lang.ref.SoftReference;

/**
 * 软引用
 * <p>
 * VM options：
 * -Xmx10m
 */
public class SoftRef {
    public static class User {
        public int id;
        public String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) {
        // 强引用对象
        User user = new User(1, "user");
        // 建立软引用
        SoftReference<User> userSoftRef = new SoftReference<User>(user);
        // 去除强引用
        user = null;
        System.out.println(userSoftRef.get());
        System.gc();

        System.out.println("After GC:");
        System.out.println(userSoftRef.get());

        byte[] bytes = new byte[1024 * 925 * 7];
        System.gc();
        System.out.println(userSoftRef.get());
    }
}
