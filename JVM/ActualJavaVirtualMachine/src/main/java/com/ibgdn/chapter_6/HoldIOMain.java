package com.ibgdn.chapter_6;

import java.io.*;

public class HoldIOMain {
    public static class HoldIOTask implements Runnable {

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            while (true) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(new File("temp"));
                    for (int i = 0; i < 10000; i++) {
                        // 大量的写操作
                        fileOutputStream.write(i);
                    }
                    fileOutputStream.close();

                    FileInputStream fileInputStream = new FileInputStream(new File("tmp"));
                    // 大量的读操作
                    while (fileInputStream.read() != -1) ;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class LazyTask implements Runnable {

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            try {
                while (true) {
                    // 一个睡眠的空闲线程
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new HoldIOTask()).start();
        new Thread(new LazyTask()).start();
        new Thread(new LazyTask()).start();
        new Thread(new LazyTask()).start();
    }
}