### 7.1 对症才能下药：找出内存溢出的原因
  内存溢出（OutOfMemory，简称 OOM），通常出现在某一块内存空间耗尽的时候。包括堆溢出、直接内存溢出、永久区溢出。

#### 7.1.1 堆溢出
  大量对象直接分配在堆空间，成为了最有可能发生溢出的空间。发生内存溢出时，通常时大量对象占据了堆空间，同时对象持有强引用，无法回收。

  [堆溢出](../java/com/ibgdn/chapter_7/SimpleHeapOOM.java)

  设置较小的堆空间将会输出如下结果：
  ```
  Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
      at com.ibgdn.chapter_7.SimpleHeapOOM.main(SimpleHeapOOM.java:9)
  ```

  `Java heap space`表示是一次堆空间溢出。可以通过增加堆空间，或者使用 MAT 、Visual VM 分析占用大量堆空间的对象，合理优化。

#### 7.1.2 直接内存溢出
  Java 的 NIO（New IO）支持直接内存的使用，即通过 Java 代码，获得一块堆外的内存空间，该空间直接向操作系统申请。直接内存的申请速度一般比堆内存空间慢，但是访问速度要快于堆内存。对于那些可复用，并且会经常访问的空间，使用直接内存可以提高系统性能。由于直接内存没有被 Java 虚拟机完全托管，使用不当容易触发直接内存溢出，导致宕机。

  [直接内存溢出](../java/com/ibgdn/chapter_7/DirectBufferOOM.java)

  ```
  [GC (Allocation Failure) [PSYoungGen: 509K->504K(1024K)] 509K->528K(1536K), 0.0008509 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 1016K->488K(1024K)] 1040K->608K(1536K), 0.0010714 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (Allocation Failure) [PSYoungGen: 995K->504K(1024K)] 1115K->688K(1536K), 0.0011863 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [GC (System.gc()) [PSYoungGen: 614K->488K(1024K)] 798K->724K(1536K), 0.0009554 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  [Full GC (System.gc()) [PSYoungGen: 488K->455K(1024K)] [ParOldGen: 236K->166K(512K)] 724K->622K(1536K), [Metaspace: 3128K->3128K(1056768K)], 0.0059841 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
  Exception in thread "main" java.lang.OutOfMemoryError: Direct buffer memory
  	at java.nio.Bits.reserveMemory(Bits.java:694)
  	at java.nio.DirectByteBuffer.<init>(DirectByteBuffer.java:123)
  	at java.nio.ByteBuffer.allocateDirect(ByteBuffer.java:311)
  	at com.ibgdn.chapter_7.DirectBufferOOM.main(DirectBufferOOM.java:14)
  ```
  出现内存溢出，Java 垃圾回收机制没有发挥作用。for 循环中分配的直接内存没有被任何对象引用，为什么没有被回收？只有直接内存使用量达到`-XX:MaxDirectMemorySize`的设置值，才会触发 GC，设置合理的`-XX:MaxDirectMemorySize`值（默认情况下等于`-Xmx`的设置）或者保证 Full GC 的执行。

#### 7.1.3 过多线程导致 OOM
  每个线程的开启都会占用系统内存，当线程数量过多时，也会导致 OOM。由于线程的栈空间在堆外分配，因此和直接内存非常相似，想让系统支持更多的线程，应该使用一个较小的堆空间（栈空间相对变大）。

  [多线程内存溢出](../java/com/ibgdn/chapter_7/MultiThreadOOM.java)

  ```
  Thread 1416 created.
        Thread 1417 created.
        Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
        at java.nio.CharBuffer.wrap(CharBuffer.java:373)
        at sun.nio.cs.StreamEncoder.implWrite(StreamEncoder.java:265)
        at sun.nio.cs.StreamEncoder.write(StreamEncoder.java:125)
        at java.io.OutputStreamWriter.write(OutputStreamWriter.java:207)
        at java.io.BufferedWriter.flushBuffer(BufferedWriter.java:129)
        at java.io.PrintStream.write(PrintStream.java:526)
        at java.io.PrintStream.print(PrintStream.java:669)
        at java.io.PrintStream.println(PrintStream.java:806)
        at com.ibgdn.chapter_7.MultiThreadOOM.main(MultiThreadOOM.java:36)
  ```
  在线程1417创建时，抛出了 OOM，表示系统创建线程的数量已经饱和，Java 进程已经达到了可用内存上限。