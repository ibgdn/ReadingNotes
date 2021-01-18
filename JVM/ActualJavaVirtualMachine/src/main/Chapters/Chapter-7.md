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

#### 7.2.2 有关 String 的内存泄漏
  什么是内存泄漏？就是由于疏忽或错误造成程序未能释放已经不再使用的内存的情况。不再使用的对象占据内存，不被释放，导致可用内存不断减少，最终导致内存溢出。

  JDK 1.6 中`java.lang.String`主要由3部分组成：代表字符数组的 value、偏移量 offset 和长度 count。由三部分共同决定一个字符串，如果出现字符串 value 包含100个字符串，实际 count 只需要1个字节，那么 String 实际只有1个字符，却占据了100个字节，99个属于内存泄漏部分，不会被使用，不会被释放，长期占用内存，直到字符串本身被回收。如果使用了`String.substring()`将一个大字符串切割为小字符串，当大字符串被回收时，小字符串的存在就会引起内存泄漏。

  在新版的`substring()`中，不再复用原 String 的 value，而是将实际需要的部分做了复制。

#### 7.2.3 有关 String 常量池的位置
  在虚拟机中，有一块称为常量池的区域，专门用于存放字符串常量。JDK 1.6之前属于永久区的一部分，JDK 1.7之后，被移到了堆中进行管理。

  [常量池溢出](../java/com/ibgdn/chapter_7/StringInternOOM.java)

  ```
  Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
  	  at java.util.Arrays.copyOf(Arrays.java:3210)
      at java.util.Arrays.copyOf(Arrays.java:3181)
      at java.util.ArrayList.grow(ArrayList.java:265)
      at java.util.ArrayList.ensureExplicitCapacity(ArrayList.java:239)
      at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:231)
      at java.util.ArrayList.add(ArrayList.java:462)
      at com.ibgdn.chapter_7.StringInternOOM.main(StringInternOOM.java:17)
  Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=5m; support was removed in 8.0
  ```

  需要注意的是，虽然`String.intern()`的返回值永远等于字符串常量。相同字符串的`intern()`返回都是一样的（95%以上的情况）。同时存在一种可能情况：在`intern()`被调用之后，该字符串在某一时刻被回收之后，再进行一次`intern()`调用，字面量相同的字符串重新被加入常量池，但是引用位置已经不同。

  [常量引用位置变动](../java/com/ibgdn/chapter_7/ConstantPool.java)

  ```
  697960108
  943010986
  1807837413
  ```
  输出三次字符串的 Hash 值，第一次为字符串本身，第二次为常量池引用，第三次为进行了常量池回收后的相同字符串的常量池引用。3次 Hash 值都是不同的。如果不进行程序当中的显示 GC 操作，后两次 Hash 值应当是相同的。

### 7.3 虚拟机也有内窥镜：使用 MAT 分析 Java 堆
  MAT 是 Memory Analyzer 的简称，功能强大的 Java 堆内存分析器，用于查找内存泄漏以及查看内存消耗的情况。基于 Eclipse 开发。
  
  [下载地址](http://www.eclipse.org/mat/)

#### 7.3.2 浅堆和深堆
  浅堆（Shallow Heap）和深堆（Retained Heap）分别表示一个对象结构所占用的内存大小和一个对象被 GC 后，可以真实释放的内存大小。

  浅堆（Shallow Heap）是指一个对象所消耗的内存。32位系统中，一个对象引用会占用4个字节，一个 int 类型会占据4个字节，long 类型变量会占据8个字节，每个对象头需要占用8个字节。

  以 String 对象为例，2个 int 值共占用8个字节，对象引用占用4个字节，对象头8字节，合计20字节，向8字节对齐，故占用24字节。与 String 对象的 value 取值无关，浅堆大小始终是24字节。

  深堆（Retained Heap）指对象的保留集中所有的对象的浅堆大小之和。保留集（Retained Set）：对象 A 的保留集指当对象 A 被垃圾回收后，可以被释放的所有对象集合（包括对象 A 本身），即对象 A 的保留集可以被认为是，只能通过对象 A 被直接或间接访问到的所有对象的集合。

  注意：浅堆指对象本身占用的内存，不包括其内部引用对象的大小。深堆是指只能通过该对象访问到的（直接或间接）所有对象的浅堆之和，即对象被回收后，可以释放的真实空间。
  
  例如，对象 A 引用了 C 和 D，对象 B 引用了 C 和 E，那么对象 A 的浅堆只是 A，深堆为 A 和 D 之和（C 可以通过 B 访问到）。