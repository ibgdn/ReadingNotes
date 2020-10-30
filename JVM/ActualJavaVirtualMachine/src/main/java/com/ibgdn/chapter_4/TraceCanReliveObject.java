package com.ibgdn.chapter_4;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * 虚引用跟踪一个可复活对象的回收
 */
public class TraceCanReliveObject {
    // finalize() 方法可以复活的对象
    public static TraceCanReliveObject object;
    static ReferenceQueue<TraceCanReliveObject> phantomQueue = null;

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
                if (phantomQueue != null) {
                    PhantomReference<TraceCanReliveObject> object = null;
                    try {
                        object = (PhantomReference<TraceCanReliveObject>) phantomQueue.remove();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (object != null) {
                        System.out.println("TraceCanReliveObject is delete by GC.");
                    }
                }
            }
        }
    }

    /**
     * Called by the garbage collector on an object when garbage collection
     * determines that there are no more references to the object.
     * A subclass overrides the {@code finalize} method to dispose of
     * system resources or to perform other cleanup.
     * <p>
     * The general contract of {@code finalize} is that it is invoked
     * if and when the Java&trade; virtual
     * machine has determined that there is no longer any
     * means by which this object can be accessed by any thread that has
     * not yet died, except as a result of an action taken by the
     * finalization of some other object or class which is ready to be
     * finalized. The {@code finalize} method may take any action, including
     * making this object available again to other threads; the usual purpose
     * of {@code finalize}, however, is to perform cleanup actions before
     * the object is irrevocably discarded. For example, the finalize method
     * for an object that represents an input/output connection might perform
     * explicit I/O transactions to break the connection before the object is
     * permanently discarded.
     * <p>
     * The {@code finalize} method of class {@code Object} performs no
     * special action; it simply returns normally. Subclasses of
     * {@code Object} may override this definition.
     * <p>
     * The Java programming language does not guarantee which thread will
     * invoke the {@code finalize} method for any given object. It is
     * guaranteed, however, that the thread that invokes finalize will not
     * be holding any user-visible synchronization locks when finalize is
     * invoked. If an uncaught exception is thrown by the finalize method,
     * the exception is ignored and finalization of that object terminates.
     * <p>
     * After the {@code finalize} method has been invoked for an object, no
     * further action is taken until the Java virtual machine has again
     * determined that there is no longer any means by which this object can
     * be accessed by any thread that has not yet died, including possible
     * actions by other objects or classes which are ready to be finalized,
     * at which point the object may be discarded.
     * <p>
     * The {@code finalize} method is never invoked more than once by a Java
     * virtual machine for any given object.
     * <p>
     * Any exception thrown by the {@code finalize} method causes
     * the finalization of this object to be halted, but is otherwise
     * ignored.
     *
     * @throws Throwable the {@code Exception} raised by this method
     * @jls 12.6 Finalization of Class Instances
     * @see WeakReference
     * @see PhantomReference
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("CanReliveObject finalize called.");
        object = this;
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "I am CanReliveObject.";
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new CheckRefQueue();
        thread.setDaemon(true);
        thread.start();

        phantomQueue = new ReferenceQueue<TraceCanReliveObject>();
        object = new TraceCanReliveObject();
        // 构建对象虚引用，并指定引用队列
        PhantomReference<TraceCanReliveObject> phantomRef = new PhantomReference<TraceCanReliveObject>(object, phantomQueue);

        // 去除对象强引用
        object = null;
        System.out.println("The first GC.");
        System.gc();
        Thread.sleep(1000);
        if (object == null) {
            System.out.println("object is null.");
        } else {
            System.out.println("object is not null.");
        }

        System.out.println("The second GC.");
        object = null;
        System.gc();
        Thread.sleep(1000);
        if (object == null) {
            System.out.println("object is null.");
        } else {
            System.out.println("object is not null.");
        }
    }
}
