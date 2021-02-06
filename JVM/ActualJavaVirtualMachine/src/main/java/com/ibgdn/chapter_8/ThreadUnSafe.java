package com.ibgdn.chapter_8;

import java.util.ArrayList;
import java.util.List;

/**
 * ArrayList 在多线程下的错误使用
 */
public class ThreadUnSafe {
    public static List<Integer> numberList = new ArrayList<>();

    public static class AddToList implements Runnable {

        int startNum = 0;

        public AddToList(int startNumber) {
            startNum = startNumber;
        }

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
            int count = 0;
            while (count < 1000000) {
                numberList.add(startNum);
                startNum += 2;
                count++;
            }
        }
    }

    public static void main(String[] args) {
        Thread threadOne = new Thread(new AddToList(0));
        Thread threadTwo = new Thread(new AddToList(1));
        threadOne.start();
        threadTwo.start();
    }
}