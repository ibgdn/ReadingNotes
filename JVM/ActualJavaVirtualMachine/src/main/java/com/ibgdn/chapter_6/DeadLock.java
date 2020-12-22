package com.ibgdn.chapter_6;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁
 */
public class DeadLock extends Thread {
    protected Object myDirect;
    static ReentrantLock south = new ReentrantLock();
    static ReentrantLock north = new ReentrantLock();

    public DeadLock(Object object) {
        this.myDirect = object;
        if (myDirect == south) {
            this.setName("south");
        }
        if (myDirect == north) {
            this.setName("north");
        }
    }

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
        if (myDirect == south) {
            // 占用 north
            try {
                north.lockInterruptibly();
                // 等待 north 启动
                Thread.sleep(500);
                // 占用 south
                south.lockInterruptibly();
                System.out.println("Car to south has passed.");
            } catch (InterruptedException e) {
                System.err.println("Car to south is killed.");
            } finally {
                if (north.isHeldByCurrentThread()) {
                    north.unlock();
                }
                if (south.isHeldByCurrentThread()) {
                    south.unlock();
                }
            }
        }

        if (myDirect == north) {
            try {
                // 占用 south
                south.lockInterruptibly();

                Thread.sleep(500);

                // 占用 north
                north.lockInterruptibly();
                System.out.println("Car to north has passed.");
            } catch (InterruptedException e) {
                System.err.println("Car to north is killed.");
            } finally {
                if (north.isHeldByCurrentThread()) {
                    north.unlock();
                }
                if (south.isHeldByCurrentThread()) {
                    south.unlock();
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        DeadLock car2South = new DeadLock(south);
        DeadLock car2North = new DeadLock(north);
        car2South.start();
        car2North.start();
        Thread.sleep(1000);
    }
}