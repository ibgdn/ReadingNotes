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

#### 7.1.5 GC 效率低下引起的 OOM
  虚拟机会检查以下几种情况：
  - 花在 GC 上的时间是否超过了98%
  - 老年代释放的内存是否小于2%
  - Eden 区释放的内存是否小于2%
  - 是否连续最近5次 GC 都同时出现了上述几种情况

  只有满足所有条件，虚拟机才有可能抛出如下 OOM：
  ```
  java.lang.OutOfMemoryError: GC overhead limit exceeded
  ```

  这个 OOM 只起辅助作用，用来帮助提示系统分配的堆空间可能大小，虚拟机不强制一定开启这个错误提示，可以通过开关`-XX:-UseGCOverheadLimit`来禁止这种 OOM 产生。

### 7.2 无处不在的字符串：String 在虚拟机中的实现
#### 7.2.1 String 对象的特点
  String 对象的特点：
  - 不变性
  - 针对常量池的优化
  - 类的 final 定义

1. 不变性
  String 对象一旦生成，无法进行改变。这个特性可以泛化成不变（immutable）模式，即一个对象的状态在对象被创建之后就不再发生变化。不变模式的主要作用在于，当一个对象需要被多线程共享，并且频繁访问时，可以省略同步和锁等待的时间，从而大幅度提高系统性能。

  由于不变性，一些看起来像是修改的操作，实际上都是依靠产生新的字符串实现的。比如`String.substring()`、`String.concat()`方法，都没有修改原始字符串，而是产生了一个新的字符串。如果需要一个可修改的字符串，可以使用 StringBuffer 或者 StringBuilder 对象。

2. 针对常量池的优化
  针对常量池的优化是指，当两个 String 对象拥有相同的值时，它们只引用常量池中的同一个拷贝。当同一个字符串反复出现时，可以大幅度节省内存空间。

  ```java
  String str1 = new String("abc");
  String str2 = new String("abc");
  System.out.println(str1 == str2);                     // false
  System.out.println(str1 == str2.intern());            // false
  System.out.println(str1.intern() == str2.intern());   // true 
  System.out.println("abc" == str2.intern());           // true 
  ```
  代码块中 str1 和 str2 都开辟了一块堆空间存放 String 对象实例，如下图所示（String 内存分配方式）。虽然 str1 和 str2 内容相同，但是在堆中的引用是不同的。`String.intern()`返回字符串在常量池中的引用，显然和 str1 也是不同的。`String.intern()`始终和常量字符串相等。
  ```mermaid
  graph LR 
  A[变量 str1] ----> B[内存空间 str 实例引用]
  C[变量 str2] ----> D[内存空间 str 实例引用]
  E[变量池 abc]
  B ----> E
  D ----> E
  ```

3. 类的 final 定义
  final 类型定义也是 String 对象的重要特点。作为 final 类的 String 对象在系统中不可能有任何子类，这是对系统安全性的保护。在 JDK 1.5之前的环境中，使用 final 定义有助于帮助虚拟机寻找机会，内联所有的 final 方法，从而提高系统效率。在 JDK 1.5之后，效果并不明显。