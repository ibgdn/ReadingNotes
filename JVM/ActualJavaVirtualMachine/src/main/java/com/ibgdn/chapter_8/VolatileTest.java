package com.ibgdn.chapter_8;

public class VolatileTest {
    public static class MyThread extends Thread {
        private boolean stop = false;
        // private volatile boolean stop = false;

        public void stopMe() {
            stop = true;
        }

        @Override
        public void run() {
            int i = 0;
            while (!stop) {
                i++;
            }
            System.out.println("Stop Thread.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MyThread myThread = new MyThread();
        myThread.start();
        Thread.sleep(1000);

        myThread.stopMe();
        Thread.sleep(1000);
    }
}