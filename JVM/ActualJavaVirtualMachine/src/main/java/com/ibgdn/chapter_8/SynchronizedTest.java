package com.ibgdn.chapter_8;

public class SynchronizedTest {
    public static class MyThread extends Thread {
        private  boolean stop = false;

        public synchronized void stopMe() {
            stop = true;
        }

        public synchronized boolean stopped() {
            return stop;
        }

        @Override
        public void run() {
            int i = 0;
            while (!stopped()) {
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
