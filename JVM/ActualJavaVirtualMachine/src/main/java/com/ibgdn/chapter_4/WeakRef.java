package com.ibgdn.chapter_4;

import java.lang.ref.WeakReference;

/**
 * 弱引用
 */
public class WeakRef {
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
        User user = new User(1, "user_1");
        WeakReference<User> userWeakReference = new WeakReference<>(user);
        user = null;

        System.out.println(userWeakReference.get());
        System.gc();

        // 不管当前可用内存空间是否足够，都会回收弱引用对象
        System.out.println("After GC:");
        System.out.println(userWeakReference.get());
    }
}
