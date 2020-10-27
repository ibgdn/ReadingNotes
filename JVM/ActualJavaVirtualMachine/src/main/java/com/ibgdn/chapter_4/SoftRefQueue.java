package com.ibgdn.chapter_4;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * 软引用引用队列
 * <p>
 * VM options：
 * -Xmx10m
 */
public class SoftRefQueue {
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

    static ReferenceQueue<User> softQueue = null;

    public static class CheckRefQueue extends Thread {
        /**
         * If this thread was constructed using a separate
         * <code>Runnable</code> run object, then that
         * <code>Runnable</code> object's <code>run</code> method is called;
         * otherwise, this method does nothing and returns.
         * <p>
         * Subclasses of <code>Thread</code> should override this method.
         *
         * @see #start()
         * @see #stop()
         * @see #Thread(ThreadGroup, Runnable, String)
         */
        @Override
        public void run() {
            while (true) {
                if (softQueue != null) {
                    UserSoftReference object = null;
                    try {
                        object = (UserSoftReference) softQueue.remove();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (object != null) {
                        System.out.println("User id " + object.uid + " is delete.");
                    }
                }
            }
        }
    }

    /**
     * 定义一个自定义软引用类
     */
    public static class UserSoftReference extends SoftReference<User> {
        int uid;

        /**
         * Creates a new soft reference that refers to the given object and is
         * registered with the given queue.
         *
         * @param referent object the new soft reference will refer to
         * @param q        the queue with which the reference is to be registered,
         */
        public UserSoftReference(User referent, ReferenceQueue<? super User> q) {
            super(referent, q);
            uid = referent.id;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new CheckRefQueue();
        thread.setDaemon(true);
        thread.start();
        User user = new User(1, "user_1");
        softQueue = new ReferenceQueue<User>();

        // 指定软引用队列，当给定的对象被回收时，会被加入引用队列，通过该队列跟踪对象的回收情况。
        UserSoftReference userSoftReference = new UserSoftReference(user, softQueue);

        user = null;
        System.out.println(userSoftReference.get());
        System.gc();

        // 内存足够，不会被回收
        System.out.println("After GC:");
        System.out.println(userSoftReference.get());

        System.out.println("Try to create byte array and GC.");
        byte[] bytes = new byte[1024 * 925 * 7];
        System.gc();
        System.out.println(userSoftReference.get());

        Thread.sleep(1000);
    }
}
