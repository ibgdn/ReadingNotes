### 3.1 一切运行都有迹可循：掌握跟踪调试参数
#### 3.1.1 跟踪垃圾回收——读懂虚拟机日志
- PrintGC
  使用 GC 参数`-XX:+PrintGC`，遇到 GC 就会打印日志。
  ```
  [GC 4793K->377K(15872KK), 0.0006926 secs]
  [GC 4857K->377K(15936KK), 0.0003595 secs]
  [GC 4857K->377K(15936KK), 0.0001755 secs]
  [GC 4857K->377K(15936KK), 0.0001957 secs]
  ```
  日志显示，一共进行了4次 GC，每次一行。GC 前堆空间使用量约为4MB；GC 后，堆空间使用量为377KB，当前可用的堆空间总和约为16MB（15936KB）。最后显示 GC 花费的时间。

- PrintGCDetails
  如果需要更加详细的信息，添加参数`-XX:+PrintGCDetails`。
  ```
  [GC (Metadata GC Threshold) [PSYoungGen: 10529K->1409K(153088K)] 10529K->1417K(502784K), 0.0039326 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1409K->0K(153088K)] [ParOldGen: 8K->1186K(133120K)] 1417K->1186K(286208K), [Metaspace: 3699K->3699K(1056768K)], 0.0194219 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 55265K->2172K(153088K)] 56452K->3366K(286208K), 0.0024525 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 2172K->0K(153088K)] [ParOldGen: 1194K->2755K(134656K)] 3366K->2755K(287744K), [Metaspace: 7443K->7443K(1056768K)], 0.0121722 secs] [Times: user=0.09 sys=0.00, real=0.01 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 34212K->1088K(153088K)] 36967K->3851K(287744K), 0.0012193 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1088K->0K(153088K)] [ParOldGen: 2763K->3065K(210432K)] 3851K->3065K(363520K), [Metaspace: 9159K->9159K(1058816K)], 0.0321379 secs] [Times: user=0.08 sys=0.00, real=0.03 secs]
  [GC (Last ditch collection) [PSYoungGen: 0K->0K(154624K)] 3065K->3065K(365056K), 0.0005435 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Last ditch collection) [PSYoungGen: 0K->0K(154624K)] [ParOldGen: 3065K->1347K(330752K)] 3065K->1347K(485376K), [Metaspace: 9159K->9159K(1058816K)], 0.0174909 secs] [Times: user=0.19 sys=0.00, real=0.02 secs]
  [GC (Metadata GC Threshold) [PSYoungGen: 2938K->64K(168448K)] 4285K->1411K(499200K), 0.0006535 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Metadata GC Threshold) [PSYoungGen: 64K->0K(168448K)] [ParOldGen: 1347K->1341K(459776K)] 1411K->1341K(628224K), [Metaspace: 9159K->9159K(1058816K)], 0.0137784 secs] [Times: user=0.06 sys=0.00, real=0.01 secs]
  [GC (Last ditch collection) [PSYoungGen: 0K->0K(153088K)] 1341K->1341K(612864K), 0.0004620 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  [Full GC (Last ditch collection) [PSYoungGen: 0K->0K(153088K)] [ParOldGen: 1341K->1341K(637952K)] 1341K->1341K(791040K), [Metaspace: 9159K->9159K(1058816K)], 0.0080811 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]

  Heap
   PSYoungGen      total 153088K, used 4577K [0x0000000755a00000, 0x0000000760a00000, 0x00000007ffe00000)
    eden space 152576K, 3% used [0x0000000755a00000,0x0000000755e786b0,0x000000075ef00000)
    from space 512K, 0% used [0x0000000760980000,0x0000000760980000,0x0000000760a00000)
    to   space 3072K, 0% used [0x0000000760400000,0x0000000760400000,0x0000000760700000)
   ParOldGen       total 637952K, used 1341K [0x0000000601200000, 0x0000000628100000, 0x0000000755a00000)
    object space 637952K, 0% used [0x0000000601200000,0x000000060134f6b0,0x0000000628100000)
   Metaspace       used 9191K, capacity 10070K, committed 10240K, reserved 1058816K
    class space    used 879K, capacity 905K, committed 1024K, reserved 1048576K
  ```

  系统经历了多次 GC，第一次是元数据 GC，回收效果是新生代从回收前的10M左右降低到1MB，整个堆从10M左右降低到1MB左右：
  ```
  [GC (Metadata GC Threshold) [PSYoungGen: 10529K->1409K(153088K)] 10529K->1417K(502784K), 0.0039326 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
  ```

  第二次是 Full GC，回收新生代、老年代、元数据，新生代被清空（不同 JDK 版本也有可能出现`1409K->1409K`，也是被清空的意思），老年代从8KB增长为1.1MB，整个堆从1.4M左右降低到1.1MB左右。日志最后，显示 GC 所花费的时间， user 表示用户态 CPU 耗时， sys 表示系统 CPU 耗时， real 表示 GC 实际经历的时间。
  ```
  [Full GC (Metadata GC Threshold) [PSYoungGen: 1409K->0K(153088K)] [ParOldGen: 8K->1186K(133120K)] 1417K->1186K(286208K), [Metaspace: 3699K->3699K(1056768K)], 0.0194219 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]
  ```

  虚拟机退出前还会打印 heap 的详细信息，记录各个区的使用情况。新生代总大小153088KB，已使用4577KB。之后的3个16进制数字表示新生代的下界、当前上界和上界。
  使用上界减去下界就能得到当前堆空间的最大值，使用当前上界减去下界就是当前虚拟机已经为程序分配的空间大小。
  如果当前上界等于下界，说明当前的堆空间已经没有扩大的可能。(0x0000000760a00000 - 0x0000000755a00000) / 1024 = 0x000000000B000000 / 1024 = 184549376 / 1024 = 180224KB（eden + from + to），153088KB（eden + from）。
  ```
  Heap
   PSYoungGen      total 153088K, used 4577K [0x0000000755a00000, 0x0000000760a00000, 0x00000007ffe00000)
    eden space 152576K, 3% used [0x0000000755a00000,0x0000000755e786b0,0x000000075ef00000)
    from space 512K, 0% used [0x0000000760980000,0x0000000760980000,0x0000000760a00000)
    to   space 3072K, 0% used [0x0000000760400000,0x0000000760400000,0x0000000760700000)
   ParOldGen       total 637952K, used 1341K [0x0000000601200000, 0x0000000628100000, 0x0000000755a00000)
    object space 637952K, 0% used [0x0000000601200000,0x000000060134f6b0,0x0000000628100000)
   Metaspace       used 9191K, capacity 10070K, committed 10240K, reserved 1058816K
    class space    used 879K, capacity 905K, committed 1024K, reserved 1048576K
  ```

- PrintHeapAtGC
  添加参数`-XX:+PrintHeapAtGC`，会在每次 GC 前后分别打印 Heap 的信息。使用这个参数，可以很好地观察 GC 对 Heap 空间的影响。

- PrintGCTimeStamps
  使用`-XX:+PrintGCTimeStamps`参数，在每次 GC 时会额外输出 GC 发生的时间，该输出时间是虚拟机启动后的时间偏移量。

- PrintGCApplicationConcurrentTime\PrintCGApplicationStoppedTime
  由于 GC 会引起应用程序停顿，可能需要特别关注应用程序的执行时间和停顿时间。使用参数`-XX:+PrintGCApplicationConcurrentTime`可以打印应用程序的执行时间。

  使用参数`-XX:+PrintCGApplicationStoppedTime`可以打印应用程序由于 GC 而产生的停顿时间。

- PrintReferenceGC
  跟踪系统内的强引用、软引用、弱引用、虚引用可以使用参数`-XX:PrintReferenceGC`。

- Xloggc
  默认情况下，GC 的日志会在控制台中输出，不便于后续分析和定位问题。添加`-Xloggc:log/gc.log`参数，虚拟机允许将 GC 日志以文件的形式输出。

#### 3.1.2 类加载/卸载的跟踪
  一般情况下，系统加载的类存在于文件系统中，以 *.jar 文件的形式打包或者以 *.class 文件的形式存在，可以直接通过文件系统查看。

  随着动态代理、AOP 等技术的普遍使用，系统也极有可能在运行时动态生成某些类，这些类相对隐蔽，无法通过文件系统找到，虚拟机提供的类加载/卸载跟踪参数就显得格外有意义。

  使用参数`-verbose:class`跟踪类的加载和卸载，也可以单独使用参数`-XX:+TraceClassLoading`跟踪类的加载，`-XX:TraceClassUnloading`跟踪类的卸载，两个参数等价。