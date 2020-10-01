### 2.1 谋全局者才能成大器：看穿 Java 虚拟机的架构
  类加载子系统负责从文件系统或者网络中加载 Class 信息，加载的 Class 信息被存放于方法区。

  方法区除了存放 Class 信息，还会存放运行时常量池信息——字符串常量和数字常量，常量信息是 Class 文件中常量池部分的内存映射。

  Java 的 NIO 库允许 Java 程序使用直接内存，直接内存存在于 Java 堆外，直接向系统申请内存空间。

  每一个 Java 虚拟机线程都有一个私有的 Java 栈。线程创建时会同时创建线程 Java 栈。

  PC（Program Counter）寄存器也是每个线程私有的空间。在任意时刻，一个 Java 线程总是在执行一个方法，正在被执行的方法被称为当前方法。如果当前方法不是本地方法，PC 寄存器就会指向当前正在被执行的指令；如果当前方法是本地方法，那么 PC 寄存器的值就是 undefined。

### 2.2 小参数能解决大问题：学会设置 Java 虚拟机的参数
  Java 进程的启动命令如下：
  ```
    java [-option] class [args...]
  ```
  -option 表示 Java 虚拟机的启动参数，class 为带有 main() 函数的 Java 类， args 表示传递给主函数 main() 的参数。

  设置最大堆内存，启动 [SimpleArgs](../java/com/ibgdn/chapter_2/SimpleArgs.java)：
  ```
    java -Xmx32m com.ibgdn.chapter_2.SimpleArgs arg
  ```
  需要切换到`/target/classes`目录下执行。

  输出结果：
  ```
  参数_1 : arg
  -Xmx: 32 M.
  ```

### 2.4 函数如何调用：出入 Java 栈
  Java 堆和程序数据密切相关，Java 栈和线程执行密切相关。

  当前正在执行的方法所对应的帧，就是当前的帧（位于栈顶），每个栈帧都保存着函数的局部变量、操作数栈和帧数据区。

  每次方法调用都会生成对应的栈帧，从而占用一定的栈空间。如果栈空间不足，函数调用自然无法继续进行下去。当请求的栈深度大于最大可用栈深度时，系统就会抛出 StackOverflowError 栈溢出错误。

  递归无线循环调用的错误示例：[StackOverflowDeep](../java/com/ibgdn/chapter_2/StackOverflowDeep.java)

  使用参数`-Xss128K`，输出结果：
  ```
  Deep of calling: 1091
  java.lang.StackOverflowError
  	at com.ibgdn.chapter_2.StackOverflowDeep.recursion(StackOverflow.java:12)
  	at com.ibgdn.chapter_2.StackOverflowDeep.recursion(StackOverflow.java:13)
  ```
  使用参数`-Xss256K`，输出结果：
  ```
  Deep of calling: 3406
  java.lang.StackOverflowError
  	at com.ibgdn.chapter_2.StackOverflowDeep.recursion(StackOverflow.java:13)
  	at com.ibgdn.chapter_2.StackOverflowDeep.recursion(StackOverflow.java:13)
  ```
#### 2.4.1 局部变量表
  局部变量表保存被调用方法的参数以及局部变量。局部变量表中的变量只在当前方法调用中有效，调用结束后，随着入栈栈帧的销毁，局部变量表也会随之销毁。

  递归无线循环调用的错误示例：[StackLocalVariableTable](../java/com/ibgdn/chapter_2/StackLocalVariableTable.java)

  使用参数`-Xss128K`，输出结果：
  ```
  Deep of calling: 302
  java.lang.StackOverflowError
  	at com.ibgdn.chapter_2.StackLocalVariableTable.recursion(StackLocalVariableTable.java:10)
   	at com.ibgdn.chapter_2.StackLocalVariableTable.recursion(StackLocalVariableTable.java:12)
  ```

  **借助 jclasslib 工具来查看。**

#### 2.4.2 操作数栈
  操作数栈主要用于保存计算过程中的中间结果，同时作为计算过程中变量临时的存储空间。

  操作数栈也是一个先进后出的数据结构，只有入栈和出栈两个操作。

#### 2.4.4 栈上分配
  栈上分配是 Java 虚拟机提供的一项优化技术，基本思想是：对于那些线程私有的对象（不可能被其他线程访问的对象），可以将他们打散分配在栈上，而不是分配在堆上。分配在栈上的好处是可以在函数调用结束后自行销毁，不需要垃圾回收器的介入，提高系统性能。

  栈上分配的技术基础是进行逃逸分析。逃逸分析的目的是判断对象的作用域是否有可能逃逸出方法体。

  栈上逃逸分析示例：[StackEscapeAnalysis](../java/com/ibgdn/chapter_2/StackEscapeAnalysis.java)

  VM options：
  ```
  -server -Xmx10m -Xms10m -XX:+DoEscapeAnalysis -XX:+PrintGC -XX:-UseTLAB -XX:-EliminateAllocations
  ```

  输出结果：
  ```
  [GC (Allocation Failure)  2844K->796K(9728K), 0.0002542 secs]
  ......
  [GC (Allocation Failure)  2844K->796K(9728K), 0.0002918 secs]
  100000000 个 User 对象花费时间：1777
  ```

  `-server` 在 Server 模式下才会启用逃逸分析；`-XX:+DoEscapeAnalysis`启用逃逸分析；`-XX:+PrintGC`打印 GC 日志；`-XX:-EliminateAllocations`开启标量替换（默认打开），允许将对象打散分配在栈上；`-XX:-UseTLAB`关闭 TLAB。

### 2.5 类去哪儿了：识别方法区
  方法区是所有线程共享的内存区域，用于保存系统的类信息，比如：类的字段、方法、常量池等。

  JDK 1.6、1.7中，方法区可以理解为永久区（Perm）。用参数`-XX:PermSize`、`-XX:MaxPermSize`指定。

  JDK 1.8中，永久区被彻底移除，取而代之的是元数据区。用参数`-XX:MaxMetaspaceSize`指定（一个大的元数据区可以使系统支持更多的类），是系统堆外的直接内存。如果不指定元数据区大小，默认情况下，虚拟机会耗尽所有的可用系统内存。

  Metaspace 溢出[Metaspace](../java/com/ibgdn/chapter_2/Metaspace.java)

  VM options：
  ```
  -XX:+PrintGCDetails -XX:MetaspaceSize=5m -XX:MaxMetaspaceSize=10m
  ```

  输出结果：
  ```
  [GC (Metadata GC Threshold) [PSYoungGen: 10529K->1376K(153088K)] 10529K->1384K(502784K), 0.0015932 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1376K->0K(153088K)] [ParOldGen: 8K->1185K(241664K)] 1384K->1185K(394752K), [Metaspace: 3698K->3698K(1056768K)], 0.0075975 secs] [Times: user=0.00 sys=0.01, real=0.01 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 55269K->2204K(153088K)] 56455K->3398K(394752K), 0.0034185 secs] [Times: user=0.08 sys=0.02, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 2204K->0K(153088K)] [ParOldGen: 1193K->2754K(437760K)] 3398K->2754K(590848K), [Metaspace: 7453K->7453K(1056768K)], 0.0128688 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 34212K->1024K(153088K)] 36966K->3786K(590848K), 0.0017898 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1024K->0K(153088K)] [ParOldGen: 2762K->3070K(675840K)] 3786K->3070K(828928K), [Metaspace: 9162K->9162K(1058816K)], 0.0323848 secs] [Times: user=0.13 sys=0.00, real=0.03 secs]
  [GC (Last ditch collection) [PSYoungGen: 0K->0K(160256K)] 3070K->3070K(836096K), 0.0005942 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Last ditch collection) [PSYoungGen: 0K->0K(160256K)] [ParOldGen: 3070K->1348K(1067520K)] 3070K->1348K(1227776K), [Metaspace: 9162K->9162K(1058816K)], 0.0175410 secs] [Times: user=0.05 sys=0.00, real=0.02 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 2990K->64K(171008K)] 4338K->1412K(1238528K), 0.0006899 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 64K->0K(171008K)] [ParOldGen: 1348K->1343K(1481728K)] 1412K->1343K(1652736K), [Metaspace: 9162K->9162K(1058816K)], 0.0167944 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]
  [GC (Last ditch collection) [PSYoungGen: 0K->0K(155648K)] 1343K->1343K(1637376K), 0.0004576 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Last ditch collection) [PSYoungGen: 0K->0K(155648K)] [ParOldGen: 1343K->1343K(2056704K)] 1343K->1343K(2212352K), [Metaspace: 9162K->9162K(1058816K)], 0.0087094 secs] [Times: user=0.01 sys=0.00, real=0.01 secs]
  Heap
   PSYoungGen      total 155648K, used 4654K [0x0000000755a00000, 0x0000000760980000, 0x00000007ffe00000)
    eden space 155136K, 3% used [0x0000000755a00000,0x0000000755e8b9e8,0x000000075f180000)
    from space 512K, 0% used [0x0000000760900000,0x0000000760900000,0x0000000760980000)
    to   space 3072K, 0% used [0x0000000760380000,0x0000000760380000,0x0000000760680000)
   ParOldGen       total 2056704K, used 1343K [0x0000000601200000, 0x000000067ea80000, 0x0000000755a00000)
    object space 2056704K, 0% used [0x0000000601200000,0x000000060134fde8,0x000000067ea80000)
   Metaspace       used 9194K, capacity 10070K, committed 10240K, reserved 1058816K
    class space    used 879K, capacity 905K, committed 1024K, reserved 1048576K
  Exception in thread "main" java.lang.IllegalStateException: Unable to load cache item
  	at net.sf.cglib.core.internal.LoadingCache.createEntry(LoadingCache.java:79)
  	at net.sf.cglib.core.internal.LoadingCache.get(LoadingCache.java:34)
  	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData.get(AbstractClassGenerator.java:119)
  	at net.sf.cglib.core.AbstractClassGenerator.create(AbstractClassGenerator.java:294)
  	at net.sf.cglib.proxy.Enhancer.createHelper(Enhancer.java:480)
  	at net.sf.cglib.proxy.Enhancer.createClass(Enhancer.java:337)
  	at com.ibgdn.chapter_2.Metaspace.main(Metaspace.java:29)
  Caused by: java.lang.OutOfMemoryError: Metaspace
  	at net.sf.cglib.core.AbstractClassGenerator.generate(AbstractClassGenerator.java:348)
  	at net.sf.cglib.proxy.Enhancer.generate(Enhancer.java:492)
  	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData$3.apply(AbstractClassGenerator.java:96)
	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData$3.apply(AbstractClassGenerator.java:94)
	at net.sf.cglib.core.internal.LoadingCache$2.call(LoadingCache.java:54)
	at java.util.concurrent.FutureTask.run$$$capture(FutureTask.java:266)
	at java.util.concurrent.FutureTask.run(FutureTask.java)
	at net.sf.cglib.core.internal.LoadingCache.createEntry(LoadingCache.java:61)
	... 6 more
  ```