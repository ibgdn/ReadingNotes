### 5.1 一心一意一件事：串行回收器
  串行回收器是指使用单线程进行垃圾回收的回收器。每次回收时，串行回收器只有一个工作线程，对于并行能力较弱的计算机来说，串行回收器的专注性和独占性往往有更好的性能表现。串行回收器可以在新生代和老年代使用，根据作用于不同的堆空间，分为新生代串行回收和老年代串行回收。
#### 5.1.1 新生代串行回收器
  串行收集器是所有垃圾收集器中最古老的一种，也是 JDK 中最基本的垃圾回收器之一。

  串行回收器的两个特点：
  - 只使用单线程进行垃圾回收。
  - 独占式垃圾回收。

  串行收集器进行垃圾回收时，Java 应用程序中的线程都需要暂停，等待垃圾回收完成。这种现象称之为“Stop-The-World”。在实时性要求较高的应用场景中，这种现象往往不能被接受。

  虽然不适用于实时性场景，但是串行回收器却是一个成熟且经过长时间生产环境考验的极为高效的收集器。**新生代串行处理器使用复制算法，实现相对简单、逻辑处理特别高效、且没有线程切换的开销。**在诸如单 CPU 处理器等硬件平台不是特别优越的场合，它的性能表现可以超过并行回收器和并发回收器。

  使用`-XX:+UseSerialGC`参数可以指定使用新生代串行收集器和老年代串行收集器。当虚拟机在 Client 模式下运行时，串行收集器是默认的垃圾收集器。

#### 5.1.2 老年代串行回收器
  **老年代串行回收器使用的是标记压缩算法。**和新生代串行回收器一样，也是串行、独占式的垃圾回收器。老年代垃圾回收通常会使用比新生代回收更长的时间，因此，在堆空间较大的应用程序中，一旦老年代串行收集器启动，应用程序很可能会因此停顿较长的时间。

  老年代串行回收器可以和多种新生代回收器配合使用，同时可以作为 CMS 回收器的备用回收器。

  启用老年代串行回收器，常用参数如下：
  - -XX:+UseSerialGC：新生代、老年代都是用串行回收器。
  - -XX:+UseParNewGC：新生代使用 ParNew 回收器，老年代使用串行回收器。
  - -XX:+UseParallelGC：新生代使用 ParallelGC 回收器，老年代使用串行回收器。

### 5.2 人多力量大：并行回收器
#### 5.2.1 新生代 ParNew 回收器
  ParNew 回收器是一个工作在新生代的垃圾回收器。简单的将串行回收器多线程化，回收策略、算法以及参数和新生代串行回收器一样。属于**独占式回收器**，垃圾收集过程中，应用程序会全部暂停。在多核 CPU 的机器上，产生的停顿时间短于串行回收器，单核 CPU 的机器由于多线程的压力，实际表现能力比串行回收器还差。

  开启 ParNew 回收器使用如下参数：
  - -XX:+UseParNewGC：新生代使用 ParNew 回收器，老年代使用串行回收器。
  - -XX:+UseConcMarkSweepGC：新生代使用 ParNew 回收器，老年代使用 CMS。

  ParNew 回收器工作时的线程数量可以使用`-XX:ParallelGCThreads`参数指定，一般设置成 CPU 核数，避免线程数量影响垃圾回收性能。**默认情况下，当 CPU 核数小于8时，ParallelGCThreads 的值等于 CPU 核数，当 CPU 核数大于8时，ParallelGCThreads 的值等于`3 + ((5 * CPU_count) / 8)`。**

#### 5.2.2 新生代 ParallelGC 回收器
  **新生代 ParallelGC 回收器也是使用复制算法的回收器。**除 ParNew 回收器一样，属于多线程、独占式的回收器之外，还有一个重要特点：关注系统吞吐量。

  新生代 ParallelGC 回收器可以使用如下参数启用：
  - -XX:+UseParallelGC：新生代使用 ParallelGC 回收器，老年代使用串行回收器。
  - -XX:+UseParallelOldGC：新生代使用 ParallelGC 回收器，老年代使用 ParallelOldGC 回收器。

  ParallelGC 回收器提供两个重要参数用于控制系统吞吐量（两个参数互相矛盾）：
  - -XX:MaxGCPauseMillis：设置最大垃圾回收停顿时间。大于0的整数。
    ParallelGC 工作时，会调整 Java 堆或其他参数的大小，尽可能将停顿时间控制在 MaxGCPauseMillis 以内。如果希望减少停顿时间，将值设置的很小，虚拟机会使用一个较小的堆空间，导致垃圾回收变得很频繁，从而增加垃圾回收总时间，降低吞吐量。
  - -XX:GCTimeRatio：设置吞吐量大小。0~100之间的整数。假设 GCTimeRatio 的值为 n，系统花费不超过`1/(1+n)`的时间用于垃圾回收。默认情况下取值是99，即不超过`1/(1+99) = 1%`的时间用于垃圾回收。

  ParallelGC 回收器于 ParNew 回收器的另一个不同之处在于支持一种自适应 GC 调节策略。使用`-XX:+UseAdaptiveSizePolicy`可以打开自适应 GC 策略。在这种模式下，新生代内存大小，Eden 区和 Survivor 区的比例，达到换代级别的年龄会被自动调整，以达到堆空间大小、吞吐量、停顿时间的平衡。

#### 5.2.3 老年代 ParallelOldGC 回收器
  老年代 ParallelOldGC 回收器也是一种多线程并发，同时关注吞吐量的回收器。是一个应用于老年代，并且和 ParallelGC 新生代回收器搭配使用的回收器。

  **ParallelOldGC 回收器使用比较压缩算法，在 JDK1.6 才使用。**

  使用参数`-XX:+UseParallelOldGC`在新生代使用 ParallelGC 回收器，在老年代使用 ParallelOldGC 回收器，是**一对非常关注吞吐量的垃圾回收组合**，对吞吐量敏感的系统可以考虑使用。参数`-XX:ParallelGCThreads`用于设置垃圾回收时的线程数量。

### 5.3 一心多用都不落下：CMS 回收器
  与 ParallelGC 和 ParallelOldGC 不同，CMS（Concurrent Mark Sweep 并发标记清除）垃圾回收器**主要关注于系统停顿时间**，标记清除算法，是一个使用多线程并行回收的垃圾回收器。

#### 5.3.1 CMS 主要工作步骤
  CMS 回收器的主要工作步骤：初始标记、并发标记、预清理、重新标记、并发清除和并发重置。初始标记、重新标记独占系统资源，预清理、并发标记、并发清除和并发重置可以和用户线程一起执行。

  ```mermaid
  graph TD
  A[初始标记 STW:标记根对象] --> B[并发标记 标记所有对象] --> C[预清理 清理前准备以及控制停顿时间] --> D[重新标记 STW:修正并发标记数据] --> E[并发清理 清理垃圾] --> F[并发重置]
  ```
  注：STW： Stop The World

  根据标记清除算法，初始标记、并发标记和重新标记都是为了标记出需要回收的对象；并发清理是在标记完成后，正式回收垃圾对象；并发重置是在垃圾回收完成后，重新初始化 CMS 数据结构和数据，为下一次垃圾回收做好准备。

  设置参数`-XX:-CMSPrecleaningEnabled`，并发标记后的预清理将会关闭。预清理时并发的，除了为正式清理做准备和检查外，预清理还会尝试控制一次停顿时间。重新标记独占 CPU，新生代 GC 后，立即触发一次重新标记，停顿的时间可能会很长。为了避免这种情况，预处理时，刻意等待一次新生代 GC，根据历史性能数据预测下一次新生代 GC 可能发生的时间，在当前时间和预测时间的中间时刻，进行重标记，最大限度避免新生代 GC 和重新标记时间重合，减少停顿时间。

#### 5.3.2 CMS 主要的设置参数
  启动 CMS 回收器的参数是`-XX:+UseConcMarkSweepGC`。默认启动的并发线程数是`(ParallelGCThreads + 3) / 4`。ParallelGCThreads 表示 GC 并行时使用的线程数量。4个 ParallelGCThreads 线程数，只有一个并发线程，5~8个 ParallelGCThreads 线程数，将会有两个并发线程。

  `-XX:ConcGCThreads`、`-XX:ParallelCMSThreads`参数可以设置并发线程数据。

  **注意：并发是指垃圾回收器和应用线程交替执行，并行是指应用程序停止，同时由多个线程一起执行 GC。**

  CMS 垃圾回收器不是独占式，它进行回收工作时，应用程序仍然在不停地工作。不会等待内存使用饱和后才进行垃圾回收，当堆内存使用率达到某一阈值便开始回收，以确保应用程序在 CMS 工作过程中，依然有足够的空间支持应用程序运行。

  参数`-XX:CMSInitiatingOccupancyFraction`指定老年代回收阈值，默认是68.当老年代的内存空间使用率达到68%时，会执行一次 CMS 回收。如果在 CMS 回收过程中出现内存空间不足的情况，CMS 回收就会失败，虚拟机将启动老年代串行收集器进行垃圾回收，应用程序将完全中断，直至垃圾回收完成，应用程序的停顿时间较长。

  如果内存使用增长缓慢，可以设置一个稍大的阈值，有效降低 CMS 的触发频率，减少老年代回收的次数，较为明显的改善应用程序的性能；如果应用程序内存使用增长很快，则应该适当降低这个阈值，以避免频繁触发老年代串行收集器。

  CMS 垃圾回收器是一个基于标记清除算法的回收器，会造成大量内存碎片，离散的可用空间无法分配较大的对象。即便堆内存仍然有较大的剩余空间，也可能会被迫进行一次垃圾回收，以换取一块可用的连续内存空间。`-XX:UseCMSCompactAtFullCollection`开关使 CMS 在垃圾收集完成后，进行一次内存碎片整理（不是并发进行）。`-XX:CMSFullGCsBeforeCompaction`参数可以用于设定进行多少次 CMS 回收后，进行一次内存压缩。

#### 5.3.3 CMS 日志分析
  CMS 回收器工作时的日志输出如下：
  ```
  1.313: [GC [1 CMS-initial-mark: 69112K(136576K)] 77037K(198016K), 0.0120453 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]
  1.325: [CMS-concurrent-mark-start]
  ...
  1.406: [CMS-concurrent-mark: 0.072/0.082 secs] [Times: user=0.17 sys=0.00, real=0.08 secs]
  1.406: [CMS-concurrent-preclean-start]
  ...
  1.409: [CMS-concurrent-abortable-preclean-start]
  ...
  1.423: [GC[YG occupancy: 35483 K (61440K)]1.423: [Rescan (parallel) , 0.0102064 secs] 1.433: [weak refs processing, 0.0000142 secs]1.433: [scrub string table, 0.0000298 secs] [1 CMS-remark: 74166K(136576K)] 109650K(198016K), 0.0103386 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]
  1.433: [CMS-concurrent-sweep-start]
  ```
  以上是一次 CMS 内存垃圾回收的部分输出信息，包括了初始化标记、并发标记、预清理、重新标记、并发清理和并发重置等几个重要阶段。

  1.409秒时，发生 abortable-preclean，表示 CMS 开始等待一次新生代 GC。之后 ParNew 垃圾回收器工作，abortable-preclean 终止（ParNew 工作信息未展示）。CMS 根据之前新生代 GC 的情况，将重新标记的时间放置在一个最不可能和下一次新生代 GC 重叠的时刻，通常为两次新生代 GC 的中间点。

  CMS 回收器在运行时还可能输出如下信息：
  ```
  33.348: [Full GC 33.348: [CMS33.347: [CMS-concurrent-sweep: 0.034/0.036 secs] [Times: user=0.11 sys=0.03, real=0.03 secs]
  (concurrent mode failure): 47066K->39901K(49152K), 0.3896802 secs] 60771K->39901K(63936K), [CMS Perm : 22529K->22529K(32768K)], 0.3897989 secs] [Times: user=0.39 sys=0.00, real=0.39 secs]
  ```

  `(concurrent mode failure)`显示 CMS 垃圾回收器并发收集失败。很可能是由于应用程序在运行过程中老年代空间不够所致。如果在 CMS 工作过程中，出现非常频繁的并发模式失败，就应该考虑进行调整，尽可能预留一个较大的老年代空间。或者设置一个较小的`-XX:CMSInitiatingOccupancyFraction`参数，降低 CMS 触发的阈值，使 CMS 在执行过程中，仍然有较大的老年代空闲空间供应用程序使用。

  **注意：CMS 回收器是一个关注停顿的垃圾回收器。同时 CMS 回收器在部分工作流程中，可以与用户程序同时运行，从而降低应用程序的停顿时间。**

### 5.4 未来我做主：G1 回收器
  G1 回收器（Garbage-First）在 JDK 1.7中正式使用的全新垃圾回收器，为了取代 CMS 回收器。G1 属于分代垃圾回收器，会区分年轻代和老年代，依然有 Eden 区和 Survivor 区，从堆的结构看，并不要求整个 Eden 区、年轻代或者老年代都连续。

  G1 使用了分区算法，特点如下：
  - **并行性**：G1 在垃圾回收期间，可以由多个 GC 线程同时工作，有效利用多核计算能力。
  - **并发性**：G1 拥有与应用程序交替执行的能力，部分工作可以和应用程序同时执行，一般来说，不会在整个回收期间完全阻塞应用程序。
  - **分代 GC**：G1 是分代垃圾回收器，同时兼顾年轻代和老年代（其他垃圾回收器，要么工作在年轻代，要么工作在老年代）。
  - **空间整理**：G1 在垃圾回收过程中，会适当的移动对象。CMS 只是简单的标记、清理对象，若干次 GC 后 CMS 必须进行一次碎片整理；G1 每次回收都会有效地复制对象，减少空间碎片。
  - **可预见性**：由于分区，G1 可以只选取部分区域进行内存垃圾回收，减少垃圾回收范围，控制全局停顿。

#### 5.4.1 G1 的内存划分和主要收集过程
  G1 回收器将堆内存进行分区，每次 GC 时，只收集其中几个区域，以此控制垃圾回收产生的停顿时间。

  G1 垃圾回收的四个阶段：
  - 新生代 GC
  - 并发标记周期
  - 混合收集
  - 如果需要，可能会进行 Full GC

#### 5.4.2 G1 的新生代 GC
  新生代 GC 的主要工作是回收 Eden 区和 Survivor 区。一旦 Eden 区被占满，新生代 GC 就会启动。新生代 GC 只处理 Eden 区和 Survivor 区，内存垃圾回收后，所有的 Eden 区都应该被清空，而 Survivor 区被收集一部分数据，至少仍然存在一个 Survivor 区（与其他新生代收集器没有太大变化）。重要变化是老年代的数据区域增多，因为部分 Survivor 区或者 Eden 区的对象可能会晋升到老年代。

  新生代 GC 发生后，如果打开了 PrintGCDetails 选项，就可以得到类似日志输出信息：
  ```
  0.336: [GC pause (young), 0.0063051 secs]
  ...
  [Eden: 235.0M(235.0M)->0.0B(229.0M) Survivors: 5120.0K->11.0M Heap:239.2M(400.0M)->10.5M(400.0M)]
  [Times: user=0.06 sys=0.00, real=0.01 secs]
  ```
  和其他内存垃圾回收器的日志相比，G1 的日志内容非常丰富。我们最为关心的是 GC 的停顿时间以及回收情况。从日志中可以看到，Eden 区原本占用235MB空间，回收后被清空，Survivor 区从5MB增长到了11MB，这是因为部分对象从 Eden 区复制到 Survivor 区，整个堆合计为400MB，从回收前的239MB下降到10.5MB。

#### 5.4.3 G1 的并发标记周期
  G1 的并发阶段和 CMS 有点类似，都是为了降低 STW 时间，将可以和应用程序并发的部分单独提取出来执行。

  并发标记周期分为如下步骤：
  - **初始标记**：标记从根节点直接可达的对象。这个阶段会伴随一次新生代 GC，会产生全局停顿，应用程序线程在这个阶段必须停止执行。
  - **根区域扫描**：初始标记必然会伴随一次新生代 GC，初始标记后，Eden 区被清空，存活对象被移入 Survivor 区。在这个阶段，将扫描由 Survivor 区直接可达的老年代区域，并标记这些直接可达的对象。这个过程可以和应用程序并发执行的，但是根区域扫描不能和新生代 GC 同时执行（因为根区域扫描依赖 Survivor 区的对象，而新生代 GC 会修改这个区域），因此如果恰巧在此时需要进行新生代 GC，就需要等待根区域扫描结束后才能进行。如果发生这种情况，这次新生代 GC 的时间就会延长。
  - **并发标记**：和 CMS 类似，并发标记将会扫描并查找整个堆的存活对象，并做好标记。这是一个并发的过程，并且这个过程可以被一次新生代 GC 打断。
  - **重新标记**：和 CMS 一样，重新标记会产生应用程序停顿。在并发标记过程中，应用程序依然在运行，标记结果可能需要进行修正，在此对上一次的标记结果进行补充。在 G1 中，这个过程使用 SATB（Snapshot-At-The-Beginning）算法完成，即 G1 会在标记之初为存活对象创建一个快照（有助于加速重新标记速度）。
  - **独占清理**：这个阶段会引起停顿。计算各个区域的存活对象和 GC 回收比例并进行排序，识别可供混合回收的区域。在这个阶段，还会更新记忆集（Remembered Set）。该阶段给出了需要被混合回收的区域并进行标记，在混合回收阶段，需要这些信息。
  - **并发清理阶段**：识别并清理完全空闲的区域。并发清理，不会引起停顿。

#### 5.4.4 混合回收
  并发标记周期中，对象回收的比例相当低；并发标记周期后，G1 已经明确知道哪些区域含有较多的垃圾对象；混合回收阶段，可以专门针对这些区域进行回收。G1（Garbage First Garbage Collector）垃圾比例高优先回收的垃圾回收器。

  混合回收既会执行正常的年轻代 GC，又会选取一些被标记的老年代区域进行回收。新生代 GC 会清空 Eden 区，另外被标记为高垃圾比例的区域也会被清空。

  混合 GC 会产生如下日志：
  ```
  1.904: [GC pause (mixed), 0.0073135 secs]
  ...
    [Eden: 4096.0K(4096.0K)->0.0B(53.0M) Survivors: 6144.0K->2048.0K Heap:127.3M(200.0M)->123.6M(200.0M)]
  [Times: user=0.00 sys=0.00, real=0.01 secs]
  ```
  混合 GC 会执行多次，直到回收足够的内存空间，之后触发一次新生代 GC。之后又可能会发生一次并发标记周期的处理，最后，引起混合 GC 执行。

  ```mermaid
  graph TD
  A[年轻代 GC] --> B[并发标记周期] --> C[混合 GC] --> A
  ```

#### 5.4.5 必要时的 Full GC
  并发垃圾回收器（CMS、G1都是）是应用程序和 GC 线程交替工作，在内存空间不足的时候会进行 Full GC。

  Full GC 日志：
  ```
  24.909: [GC concurrent-mark-start]
  24.909: [Full GC 898M->896M(900M), 0.7505595 secs]
      [Eden: 0.0B(45.0M)->0.0B(45.0M) Survivors: 0.0B->0.0B Heap: 898.7M(900.0M)->896.2M(900.0M)]
    [Times: user=1.05 sys=0.00, real=0.75 secs]
  25.660: [GC concurrent-mark-abort]
  ```
  如果在混合 GC 时发生空间不足或者在新生代 GC 时，Survivor 区和老年代无法容纳幸存对象，都会导致一次 Full GC 发生。

#### 5.4.6 G1 日志
  完整 G1 新生代日志：
  ```
  1.619: [GC pause (young) (initial-mark), 0.03848843 secs]
    [Parallel Time: 38.0ms]
      [GC Worker Start (ms): 1619.3 1619.3 1619.3 1619.3
       Avg: 1619.3, Min:    1619.3, Max:    1619.3, Diff:   0.0]
      [Ext Root Scanning (ms): 0.3 0.3 0.2 0.2
       Avg: 0.3, Min:   0.2, Max: 0.3, Diff:  0.1]
      [Update RS (ms): 5.7 5.4 28.0 5.3
       Avg: 11.1, Min:  5.3, Max:   28.0, Diff: 22.8]
         [Processed Buffers : 5 4 1 4
          Sum: 14, Avg: 3, Min: 1, Max: 5, Diff: 4]
      [Scan RS (ms): 4.6 5.0 0.0 5.2
       Avg: 3.7, Min:   0.0, Max: 5.2, Diff:  5.2]
      [Object Copy (ms): 27.4 27.3 9.6 27.2
       Avg: 22.9, Min:  9.6, Max:   27.4, Diff: 17.7]
      [Termination (ms): 0.1 0.0 0.0 0.1
       Avg: 0.0, Min:   0.0, Max: 0.1, Diff:  0.1]
         [Termination Attempts : 3 1 10 5
          Sum: 19, Avg: 4, Min: 1, Max: 10, Diff: 9]
      [GC Worker End (ms): 1657.3 1657.2 1657.2 1657.2
       Avg: 1657.2, Min:    1657.2, Max:    1657.3, Diff:   0.0]
      [GC Worker (ms): 38.0 38.0 38.0 38.0
       Avg: 38.0, Min:  38.0, Max:   38.0, Diff:   0.1]
      [GC Worker Other (ms): 0.0 0.1 0.1 0.1
       Avg: 0.1, Min:   0.0, Max:   0.1, Diff:  0.1]
    [Clear CT:  0.0 ms]
    [Other: 0.4 ms]
       [Choose CSet:    0.0 ms]
       [Ref Proc:   0.1 ms]
       [Ref Enq:    0.1 ms]
       [Free CSet:  0.1 ms]
    [Eden: 32M(35M)->0B(35M) Survivors: 5120K->5120K Heap: 147M(200M)->147M(200M)]
  [Times: user=0.16 sys=0.00, real=0.04 secs]
  ```

  - 新生代 GC
      ```
      1.619: [GC pause (young) (initial-mark), 0.03848843 secs]
      ```
      应用程序开启 1.619 秒时发生了一次新生代 GC，初始标记时发生的，耗时0.038秒，应用程序至少暂停了0.038秒。
  - 后续并行时间
      ```
        [Parallel Time: 38.0ms]
      ```
      所有 GC 线程总的花费时间，38毫秒。
  - 每一个线程的执行情况
      ```
          [GC Worker Start (ms): 1619.3 1619.3 1619.3 1619.3
           Avg: 1619.3, Min:    1619.3, Max:    1619.3, Diff:   0.0]
      ```
      一共4个 GC 线程（第一行有4个数据），都在1619.3秒时启动。给出几个启动数据的统计值，平均（Avg）、最小（Min）、最大（Max）和差值（Diff，最大值和最小值的差值）。
  - 根扫描耗时
      ```
          [Ext Root Scanning (ms): 0.3 0.3 0.2 0.2
           Avg: 0.3, Min:   0.2, Max: 0.3, Diff:  0.1]
      ```
      根扫描时（全局变量、系统数据字典、线程栈等），每一个 GC 线程的耗时。第一行给出分配消耗时间，第二行给出耗时的统计数据。
  - 更新记忆集（Remembered Sets）耗时
      ```
          [Update RS (ms): 5.7 5.4 28.0 5.3
           Avg: 11.1, Min:  5.3, Max:   28.0, Diff: 22.8]
             [Processed Buffers : 5 4 1 4
              Sum: 14, Avg: 3, Min: 1, Max: 5, Diff: 4]
      ```
      记忆集是 G1 中的一个数据结构，简称 RS。每个 G1 区域都有一个 RS 与之关联。G1 进行垃圾回收时按照区域回收，扫描多个区域来判定对象是否可达，代价较高。G1 在 A 区域的 RS 中，记录了在 A 区域中被其他区域引用的对象，在回收 A 区域的对象时，只需要将 RS 视为 A 区域根集的一部分，从而避免做整个堆空间的扫描。

      由于系统在运行过程中，对象之间的引用关系时刻变化，为了更高效追踪这些引用关系，将这些变化记录在 Update Buffers 中。Processed Buffers 指的就是处理这个 Update Buffers 数据。4个时间和就是4个 GC 线程的耗时，以及统计数据。更新 RS 时，分别耗时5.7、5.4、28、5.3毫秒，平均耗时11.1毫秒。
  - 扫描 RS 的时间
    ```
          [Scan RS (ms): 4.6 5.0 0.0 5.2
           Avg: 3.7, Min:   0.0, Max: 5.2, Diff:  5.2]
    ```
  - 正式回收时，G1 会对被回收区域的对象进行疏散，通过复制将存活对象放置在其他区域。
    ```
          [Object Copy (ms): 27.4 27.3 9.6 27.2
           Avg: 22.9, Min:  9.6, Max:   27.4, Diff: 17.7]
    ```
    Object Copy 就是对象赋值的耗时。
  - GC 工作线程的终止信息
    ```
          [Termination (ms): 0.1 0.0 0.0 0.1
           Avg: 0.0, Min:   0.0, Max: 0.1, Diff:  0.1]
             [Termination Attempts : 3 1 10 5
              Sum: 19, Avg: 4, Min: 1, Max: 10, Diff: 9]
    ```
    终止时间是线程花在终止阶段的耗时。GC 线程终止前，会检查其他 GC 线程的工作队列，查看是否仍突然还有对象引用没有处理完，如果其他线程仍有没有处理完的数据，请求终止的 GC 线程就会帮助它尽快完成，随后再尝试终止。Termination Attempts 展示每个线程尝试终止的次数。
  - GC 工作线程的完成时间
    ```
          [GC Worker End (ms): 1657.3 1657.2 1657.2 1657.2
           Avg: 1657.2, Min:    1657.2, Max:    1657.3, Diff:   0.0]
    ```
    1658毫秒之后，4个线程都终止了。
  - 4个 GC 工作线程的存活时间
    ```
          [GC Worker (ms): 38.0 38.0 38.0 38.0
           Avg: 38.0, Min:  38.0, Max:   38.0, Diff:   0.1]
    ```
  - GC 花费在其他任务中的耗时
    ```
          [GC Worker Other (ms): 0.0 0.1 0.1 0.1
           Avg: 0.1, Min:   0.0, Max:   0.1, Diff:  0.1]
    ```
  - 清空 CardTable 的时间，RS 就是依靠 CardTable 来记录存活对象
    ```
        [Clear CT:  0.0 ms]
    ```
  - 其他几个任务的耗时
    ```
        [Other: 0.4 ms]
           [Choose CSet:    0.0 ms]
           [Ref Proc:   0.1 ms]
           [Ref Enq:    0.1 ms]
           [Free CSet:  0.1 ms]
    ```
    选择 CSet（Collection Sets）的时间、Ref Proc（处理弱引用、软引用的时间）、Ref Enq（弱引用、软引用入队时间）和 Free CSet（释放被回收的 CSet 中区域的时间，包括它们的 RS）。
    **注意：Collection Sets 表示被选取的、将要被收集的区域的集合。**
  - GC 回收的整体情况
    ```
        [Eden: 32M(35M)->0B(35M) Survivors: 5120K->5120K Heap: 147M(200M)->147M(200M)]
      [Times: user=0.16 sys=0.00, real=0.04 secs]
    ```
    Eden 区一共32MB被清空，Survivor 区没有释放对象，整个堆空间没有释放空间。用户 CPU 耗时0.16秒，实际耗时0.04秒。

#### 5.4.7 G1 相关的参数
  参数`-XX:+UseG1GC`标记打开 G1 垃圾回收器开关；参数`-XX:MaxGCPauseMillis`指定目标最大停顿时间，超过这个设定值，G1 就会尝试调整新生代和老年代的比例、堆空间大小、换代年龄等，来达到预设数值。  对于性能调优来说，如果停顿时间缩短，可能就要增加新生代 GC 的次数，变得更加频繁；老年代区域为了获得更短的停顿时间，在混合 GC 收集时，一次收集的区域数量也会变少，无疑增加了进行 Full GC 的可能性。

  参数`-XX:ParallelGCThreads`用于设置并行回收时，GC 的工作线程数量。

  参数`-XX:InitiatingHeapOccupancyPercent`可以指定当整个堆使用率达到设定值时，触发并发标记周期的执行。默认是45，即当整个堆空间使用率达到45%时，执行并发标记周期。InitiatingHeapOccupancyPercent 一旦设置，始终不会被 G1 垃圾回收器修改，这意味着 G1 垃圾回收器不会为满足 MaxGCPauseMillis 来修改这个值。如果 InitiatingHeapOccupancyPercent 值偏大，会导致并发周期迟迟得不到启动，引起 Full GC 的可能性大大增加；如果值偏小，会使得并发周期非常频繁，大量 GC 线程抢占 CPU，应用程序性能下降。

### 5.5 回眸：有关对象内存分配和回收的一些细节问题
#### 5.5.1 禁用 System.gc()
  默认情况下，`System.gc()`会显式直接触发 Full GC，同时对新生代和老年代内存垃圾进行回收。一般情况下，内存垃圾回收应该是自动进行，无需手动触发。参数`DisableExplicitGC`控制是否手动触发 GC。

  手动触发方式：
  ```java
  Runtime.getRuntime().gc();
  ```
  `Runtime.gc()`是一个 Native 方法，最终实现在 jvm.cpp 中
  ```
  JVM_ENTRY_NO_ENV(void, JVM_GC(void))
    JVMWrapper("JVM_GC");
    if (!DisableExplicitGC) {
      Universe::heap()->collect(GCCause::_java_lang_system_gc);
    }
  JVM_END
  ```
  设置了`-XX:-+DisableExplicitGC`，条件判断无法成立，禁用显式 GC，`System.gc()`等价于一个空函数调用。
  
#### 5.5.2 System.gc() 使用并发回收
  默认情况，`System.gc()`生效，会使用传统的 Full GC 方式回收整个堆空间，忽略了参数中的 UseG1GC 和 UseConcMarkSweepGC。
  
  `-XX:+PrintGCDetails -XX:+UseConcMarkSweepGC` 或者 `-XX:+PrintGCDetails -XX:+UseG1GC`遇到`System.gc()`时，就会输出日志：
  CMS：
  ```
  [Full GC[CMS: 454K->453K(10944K), 0.0046875 secs] 544K->453K(15936K), [CMS Perm : 1593K->1593K(12288K)],  0.0047210 secs] [Times: user=0.02   sys=0.00,   real=0.01 secs]
  ```

  G1：
  ```
  [Full GC 616K->453K(5120K), 0.0049140 secs]
      [Eden: 1024.0K(7168.0K)->0.0B(2048.0K) Survivors: 0.0B->0.0B Heap: 616.5K(16.0M)->453.4K(5120.0K)]
    [Times: user= 0.01  sys=0.00,   real=0.00 secs]
  ```
  CMS 和 G1 都没有并发执行垃圾回收，因为在日志中没有任何并发相关信息。通过参数`-XX:+ExplicitGCInvokesConcurrent`可以改变默认设置：
  
  `-XX:+PrintGCDetails -XX:UseConcMarkSweepGC -XX:+ExplicitGCInvokesConcurrent`或者`-XX:+PrintGCDetails -XX:+UseG1GC -XX:+ExplicitGCInvokesConcurrent`
  
  CMS：
  ```
  [GC[ParNew: 620K->462K(4928K), 0.0012471 secs] 620K->462K(15872K), 0.0012948 secs] [Times: user=0.00  sys=0.00, real=0.00 secs]
  [GC [1 CMS-initial-mark: 0K(10944K)] 462K(15872K), 0.0004039 secs] [Times: user=0.00  sys=0.00,   real=0.00 secs]
  [CMS-concurrent-mark: 0.006/0.006 secs] [Times: user=0.01 syst=0.00, real=0.01 secs]
  [CMS-concurrent-preclean: 0.000/0.000 secs] [Times: user=0.00 sys=0.00,   real=0.00 secs]
  [GC[YG occupancy: 550 K (4928 K)][Rescan (parallel) , 0.0002013 secs] [weak refs processing, 0.0000060 secs] [scrub string table, 0.0000209 secs] [1 CMS-remark: 0k(10944K)] 550K(15872K), 0.0002639 secs] [Times: user=0.00  sys=0.00,   real=0.00 secs]
  [CMS-concurrent-sweep: 0.000/0.000 secs] [Times: user=0.00    sys=0.00,   real=0.00 secs]
  ```

  G1（部分省略）：
  ```
  [GC pause (young) (initial-mark), 0.0013322 secs]
      [Parallel Time: 1.1 ms, GC Workers: 2]
      [Eden: 1024.0K(7168.0K)->0.0B(5120.0K) Survivors: 0.0B->1024.0K Heap:616.5K(16.0M)->476.1K(16.0M)]
    [Times: user=0.00   sys=0.00,   real=0.00 secs]
  [GC concurrent-root-region-scan-start]
  [GC concurrent-root-region-scan-end, 0.0003496 secs]
  [GC concurrent-mark-start]
  [GC concurrent-mark-end, 0.0000331 secs]
  [GC remark [GC ref-proc, 0.0000142 secs], 0.0003168 secs]
    [Times: user=0.00   sys=0.00,   real=0.00 secs]
  [GC cleanup 517K->517K(16M), 0.0000742 secs]
    [Times: user=0.00   sys=0.00,   real=0.00 secs]
  ```
  只有设置`ExplicitGCInvokesConcurrent`参数后，`System.gc()`显示 GC 才会使用并发方式进行回收。否则，不管使用哪种内存垃圾回收器，都不会进行并发回收。

#### 5.5.3 并行 GC 前额外触发的新生代 GC
  并行垃圾回收器的 Full GC（使用 UseParallelOldGC 或者 UseParallelGC），在每次 Full GC 之前都会伴随一次新生代 GC。
  
  ```java
  System.gc();
  ```
  代码只是进行一次简单的 Full GC，使用串行垃圾回收器`-XX:+PrintGCDetails -XX:+UseSerialGC`系统 GC 日志输出如下：
  ```
  [Full GC[Tenured: 0K->376K(10944K), 0.003328 secs] 603K->376K(15872K), [Perm: 142K->142K(12288K)], 0.033825 secs] [Times: user=0.00   sys=0.00,   real=0.00 secs]
  ```

  切换并行垃圾回收器`-XX:+PrintGCDetails -XX:+UseParallelOldGC`GC 日志输出如下：
  ```
  [GC [PSYoungGen: 670K->480K(5120K)] 670K->480K(15872K), 0.0153729 secs] [Times: user=0.02 sys=0.00,   real=0.02 secs]
  [Full GC [PSYoungGen: 480K->0K(5120K)] [ParOldGen: 0K->453K(10752K)] 480K->453K(15872K) [PSPermGen: 1592K->1591K(12288K)], 0.0073608 secs] [Times: user=0.00  sys=0.00,   real=0.01 secs]
  ````
  使用并行垃圾回收器时，触发 Full GC 之前，进行一次新生代 GC。`System.gc()`实际触发了两次 GC。先将新生代对象进行一次垃圾回收，避免将所有垃圾回收工作同时交给 Full GC 进行，尽可能的缩短停顿时间。
  
  如果不需要这个特性，使用参数`-XX:-ScavengeBeforeFullGC`（默认为 true）去除发生在 Full GC 之前的那次新生代 GC。

#### 5.5.4 对象何时进入老年代
  对象被创建出来后，一般首先会放置在新生代的 Eden（伊甸园）区。 圣经记载，亚当和夏娃住在伊甸园，也就是人类开始居住的地方。如果没有 GC 介入，对象将不会离开 Eden 区。

1. 初创的对象在 Eden 区
  
  垃圾回收器的任务是识别和回收垃圾对象进行内存清理。为了让垃圾回收器正常高效的工作，大部分情况下需要系统进入一个停顿的状态。停顿的目的是终止所有应用线程的执行（只有这样，系统才不会产生新的垃圾），同时停顿保证了系统状态在某一个瞬间的一致性，方便垃圾回收器标记垃圾对象。停顿产生时，整个应用程序都会被卡死，没有任何响应，因此这个停顿也叫“Stop-The-World”（STW）。

  AllocEden：[AllocEden](../java/com/ibgdn/chapter_5/AllocEden.java)

  VM options：
  ```
  -Xmx64M -Xms64M -XX:+PrintGCDetails
  ```

  输出内容：
  ```
  Heap
   PSYoungGen      total 18944K, used 7884K [0x00000000feb00000, 0x0000000100000000, 0x0000000100000000)
    eden space 16384K, 48% used [0x00000000feb00000,0x00000000ff2b3110,0x00000000ffb00000)
    from space 2560K, 0% used [0x00000000ffd80000,0x00000000ffd80000,0x0000000100000000)
    to   space 2560K, 0% used [0x00000000ffb00000,0x00000000ffb00000,0x00000000ffd80000)
   ParOldGen       total 44032K, used 0K [0x00000000fc000000, 0x00000000feb00000, 0x00000000feb00000)
    object space 44032K, 0% used [0x00000000fc000000,0x00000000fc000000,0x00000000feb00000)
   Metaspace       used 3048K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  `eden space 16384K, 48% used [0x00000000feb00000,0x00000000ff2b3110,0x00000000ffb00000)`
  没有发生 GC，创建的对象全部放在堆中， Eden 区占据了8M左右的空间，From、To 区和 ParOldGen（老年代）均没有使用。

2. 老年对象进入老年代
  对象的年龄达到一定数值，就会由年轻代进入老年代，被称为“晋升”。对象的年龄由对象经历过的 GC 次数决定，经历一次 GC，同时没有被回收，年龄就会加1，`MaxTenuringThreshold`控制新生代对象最大年龄（默认为15），经历过15次 GC 的新生代对象就会进入老年代。

  MaxTenuringThreshold：[MaxTenuringThreshold](../java/com/ibgdn/chapter_5/MaxTenuringThreshold.java)
  创建数组，并放入集合，防止被 GC 回收，同时不断地进行内存分配，触发新生代 GC。
  
  VM options：
  ```
  -Xmx1024M -Xms1024M -XX:+PrintGCDetails -XX:MaxTenuringThreshold=15 -XX:+PrintHeapAtGC
  ```

  输出内容：
  ```
  {Heap before GC invocations=1 (full 0):
   PSYoungGen      total 305664K, used 261493K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 262144K, 99% used [0x00000000eab00000,0x00000000faa5d5b8,0x00000000fab00000)
    from space 43520K, 0% used [0x00000000fd580000,0x00000000fd580000,0x0000000100000000)
    to   space 43520K, 0% used [0x00000000fab00000,0x00000000fab00000,0x00000000fd580000)
   ParOldGen       total 699392K, used 0K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0000000,0x00000000eab00000)
   Metaspace       used 3051K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 321K, capacity 392K, committed 512K, reserved 1048576K
  [GC (Allocation Failure) [PSYoungGen: 261493K->6271K(305664K)] 261493K->6271K(1005056K), 0.0069430 secs] [Times: user=0.00 sys=0.00, real=0.02 secs] 
  Heap after GC invocations=1 (full 0):
   PSYoungGen      total 305664K, used 6271K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 262144K, 0% used [0x00000000eab00000,0x00000000eab00000,0x00000000fab00000)
    from space 43520K, 14% used [0x00000000fab00000,0x00000000fb11fcc0,0x00000000fd580000)
    to   space 43520K, 0% used [0x00000000fd580000,0x00000000fd580000,0x0000000100000000)
   ParOldGen       total 699392K, used 0K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0000000,0x00000000eab00000)
   Metaspace       used 3051K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 321K, capacity 392K, committed 512K, reserved 1048576K
  }
  {Heap before GC invocations=2 (full 0):
   PSYoungGen      total 305664K, used 268415K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 262144K, 100% used [0x00000000eab00000,0x00000000fab00000,0x00000000fab00000)
    from space 43520K, 14% used [0x00000000fab00000,0x00000000fb11fcc0,0x00000000fd580000)
    to   space 43520K, 0% used [0x00000000fd580000,0x00000000fd580000,0x0000000100000000)
   ParOldGen       total 699392K, used 0K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0000000,0x00000000eab00000)
   Metaspace       used 3051K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 321K, capacity 392K, committed 512K, reserved 1048576K
  [GC (Allocation Failure) [PSYoungGen: 268415K->6232K(305664K)] 268415K->6240K(1005056K), 0.0031253 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  Heap after GC invocations=2 (full 0):
   PSYoungGen      total 305664K, used 6232K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 262144K, 0% used [0x00000000eab00000,0x00000000eab00000,0x00000000fab00000)
    from space 43520K, 14% used [0x00000000fd580000,0x00000000fdb96040,0x0000000100000000)
    to   space 43520K, 0% used [0x00000000fab00000,0x00000000fab00000,0x00000000fd580000)
   ParOldGen       total 699392K, used 8K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0002000,0x00000000eab00000)
   Metaspace       used 3051K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 321K, capacity 392K, committed 512K, reserved 1048576K
  }
  ... ...
  {Heap before GC invocations=15 (full 0):
   PSYoungGen      total 339968K, used 330190K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 330752K, 99% used [0x00000000eab00000,0x00000000fed73bd0,0x00000000fee00000)
    from space 9216K, 0% used [0x00000000ff700000,0x00000000ff700000,0x0000000100000000)
    to   space 9216K, 0% used [0x00000000fee00000,0x00000000fee00000,0x00000000ff700000)
   ParOldGen       total 699392K, used 6536K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0662070,0x00000000eab00000)
   Metaspace       used 3052K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 321K, capacity 392K, committed 512K, reserved 1048576K
  [GC (Allocation Failure) [PSYoungGen: 330190K->0K(339968K)] 336727K->6536K(1039360K), 0.0003203 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
  Heap after GC invocations=15 (full 0):
   PSYoungGen      total 339968K, used 0K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 330752K, 0% used [0x00000000eab00000,0x00000000eab00000,0x00000000fee00000)
    from space 9216K, 0% used [0x00000000fee00000,0x00000000fee00000,0x00000000ff700000)
    to   space 9216K, 0% used [0x00000000ff700000,0x00000000ff700000,0x0000000100000000)
   ParOldGen       total 699392K, used 6536K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0662070,0x00000000eab00000)
   Metaspace       used 3052K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 321K, capacity 392K, committed 512K, reserved 1048576K
  }
  Heap
   PSYoungGen      total 339968K, used 189349K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 330752K, 57% used [0x00000000eab00000,0x00000000f63e95d0,0x00000000fee00000)
    from space 9216K, 0% used [0x00000000fee00000,0x00000000fee00000,0x00000000ff700000)
    to   space 9216K, 0% used [0x00000000ff700000,0x00000000ff700000,0x0000000100000000)
   ParOldGen       total 699392K, used 6536K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
    object space 699392K, 0% used [0x00000000c0000000,0x00000000c0662070,0x00000000eab00000)
   Metaspace       used 3058K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  分配1G内存，将对象尽可能预留在新生代（堆空间大，新生代就大）。显示展示`MaxTenuringThreshold`，打开`PrintHeapAtGC`，GC 时打印堆详细信息。
  
  第一次 GC 开始前， Eden 区使用了99%（触发 GC 的原因）。Eden 区不能容纳更多对象，又有新的对象产生，就需要对 Eden 区进行清理，将存活的对象移入 From 区，From 区占用了14%，`43520K * 14% = 6092.8K`，接近6MB，放在 map 对象中的 byte 数组匹配，第一次 GC 的另一个影响是 Eden 区被清空。每一次 GC 都会使存活对象年龄加1，15次之后，新生代被清空。
  ```
   PSYoungGen      total 339968K, used 0K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
    eden space 330752K, 0% used [0x00000000eab00000,0x00000000eab00000,0x00000000fee00000)
    from space 9216K, 0% used [0x00000000fee00000,0x00000000fee00000,0x00000000ff700000)
    to   space 9216K, 0% used [0x00000000ff700000,0x00000000ff700000,0x0000000100000000)
  ```
  新生代被移除的对象，晋升到老年代（map 对象中的 bytes 数组），老年代有`6536KB`被使用，新生代使用为`0KB`，说明新生代对象进入了老年代。
  
  计算晋升年龄的逻辑代码如下：
  ```
  size_t desired_survivor_size = (size_t)((((double) survivor_capacity) * TargetSurvivorRatio) / 100);
  size_t total = 0;
  int age = 1;
  assert(sizes[0] == 0, "no objects with age zero should be recorded");
  while (age < table_size) {
    total += sizes[age];
    // check if including objects of age 'age' made us pass the desired size, if so 'age' is the new threshold
    if (total > desired_survivor_size) break;
    age++;
  }
  int result = age < MaxTenuringThreshold ? age : MaxTenuringThreshold;
  ```
  `desired_survivor_size `定义了期望的`survivor`区的使用大小，`while`循环计算对象晋升年龄，`sizes`数组保存每一个年龄段的对象大小之和。在`age`和`MaxTenuringThreshold`中取较小者作为对象的实际晋升年龄，确定对象晋升的另外一个重要参数是`TargetSurvivorRation`，用于设置 Survivor 区的目标使用率，默认为50，如果 Survivor 区在 GC 后超过50%的使用率，很有可能会使用较小的`age`作为晋升年龄。
  
  VM options：
  ```
  -Xmx1024M -Xms1024M -XX:+PrintGCDetails -XX:MaxTenuringThreshold=15 -XX:+PrintHeapAtGC -XX:TargetSurvivorRatio=13
  ```
  如果更改`TargetSurvivorRatio`的值为13，小于14%，会帮助`map`对象更快的进入老年代。
  
  **注意：对象的实际晋升年龄是根据 Survivor 区的使用情况动态计算得来的，`MaxTenuringThreshold` 只是表示对象年龄的最大值。**
  
3. 大对象进入老年代
  除了对象的年龄外，体积也影响对象的晋升。新生代 Eden 区或者 Survivor 区都无法容纳的对象，会直接晋升到老年代。

  参数`PretenureSizeThreshold`设置对象直接晋升到老年代的阈值，单位字节。只对串行回收器和 ParNew 有效，对 ParallelGC 无效。默认值为0，没有固定值，由运行决定。

  PretenureSizeThreshold：[PretenureSizeThreshold](../java/com/ibgdn/chapter_5/PretenureSizeThreshold.java)

  VM options：
  ```
  -Xmx32m -Xms32m -XX:+UseSerialGC -XX:+PrintGCDetails
  ```

  输出内容：
  ```
  Heap
   def new generation   total 9792K, used 7974K [0x00000000fe000000, 0x00000000feaa0000, 0x00000000feaa0000)
    eden space 8704K,  91% used [0x00000000fe000000, 0x00000000fe7c9838, 0x00000000fe880000)
    from space 1088K,   0% used [0x00000000fe880000, 0x00000000fe880000, 0x00000000fe990000)
    to   space 1088K,   0% used [0x00000000fe990000, 0x00000000fe990000, 0x00000000feaa0000)
   tenured generation   total 21888K, used 0K [0x00000000feaa0000, 0x0000000100000000, 0x0000000100000000)
     the space 21888K,   0% used [0x00000000feaa0000, 0x00000000feaa0000, 0x00000000feaa0200, 0x0000000100000000)
   Metaspace       used 3057K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  `def new generation   total 9792K, used 7974K [0x00000000fe000000, 0x00000000feaa0000, 0x00000000feaa0000)`
  所有的对象均分配在新生代，老年代使用率为0。
  
  VM options：
  ```
  -Xmx32m -Xms32m -XX:+UseSerialGC -XX:+PrintGCDetails -XX:PretenureSizeThreshold=1000
  ```

  输出内容：
  ```
  Heap
   def new generation   total 9792K, used 7869K [0x00000000fe000000, 0x00000000feaa0000, 0x00000000feaa0000)
    eden space 8704K,  90% used [0x00000000fe000000, 0x00000000fe7af5e8, 0x00000000fe880000)
    from space 1088K,   0% used [0x00000000fe880000, 0x00000000fe880000, 0x00000000fe990000)
    to   space 1088K,   0% used [0x00000000fe990000, 0x00000000fe990000, 0x00000000feaa0000)
   tenured generation   total 21888K, used 104K [0x00000000feaa0000, 0x0000000100000000, 0x0000000100000000)
     the space 21888K,   0% used [0x00000000feaa0000, 0x00000000feaba150, 0x00000000feaba200, 0x0000000100000000)
   Metaspace       used 3058K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  `
   def new generation   total 9792K, used 7869K [0x00000000fe000000, 0x00000000feaa0000, 0x00000000feaa0000)
   tenured generation   total 21888K, used 104K [0x00000000feaa0000, 0x0000000100000000, 0x0000000100000000)
  `
  之前期望至少5MB数据分配到老年代，作为分配主体的数据看起来依然在新生代，似乎 PretenureSizeThreshold 不起作用，只是老年代略有不同，只有104KB被使用。

  其实出现这种情况是虚拟机在为线程分配空间时，优先使用 TLAB 的区域，对于体积不大的对象，会在 TLAB 区域先行分配，因此失去了在老年代分配的机会。禁用 TLAB 即可。

  VM options：
  ```
  -Xmx32m -Xms32m -XX:+UseSerialGC -XX:+PrintGCDetails -XX:-UseTLAB -XX:PretenureSizeThreshold=1000
  ```

  输出内容：
  ```
  Heap
   def new generation   total 9792K, used 1299K [0x00000000fe000000, 0x00000000feaa0000, 0x00000000feaa0000)
    eden space 8704K,  14% used [0x00000000fe000000, 0x00000000fe144d40, 0x00000000fe880000)
    from space 1088K,   0% used [0x00000000fe880000, 0x00000000fe880000, 0x00000000fe990000)
    to   space 1088K,   0% used [0x00000000fe990000, 0x00000000fe990000, 0x00000000feaa0000)
   tenured generation   total 21888K, used 6194K [0x00000000feaa0000, 0x0000000100000000, 0x0000000100000000)
     the space 21888K,  28% used [0x00000000feaa0000, 0x00000000ff0aca00, 0x00000000ff0aca00, 0x0000000100000000)
   Metaspace       used 3057K, capacity 4556K, committed 4864K, reserved 1056768K
    class space    used 322K, capacity 392K, committed 512K, reserved 1048576K
  ```
  `
   tenured generation   total 21888K, used 6194K [0x00000000feaa0000, 0x0000000100000000, 0x0000000100000000)
  `
  禁用 TLAB 后，大于1000字节的 byte 数组分配到了老年代。

#### 5.5.5 在 TLAB 上分配对象
  TLAB 的全称时 Thread Local Allocation Buffer，线程本地分配缓存，是一个线程专用的内存分配区域。

  为了加速对象分配，出现了这个区域。对象一般会分配到堆上，全局共享，同一时间可能会有多个线程在堆上申请空间。每一次对象分配都需要进行同步，在竞争激烈的场合分配的效率会进一步下降，Java 虚拟机使用 TLAB 线程专属的区域来避免线程冲突，提高对象分配的效率。TLAB 本身占用的是 Eden 区的空间。

  UseTLAB：[UseTLAB](../java/com/ibgdn/chapter_5/UseTLAB.java)

  VM options：
  ```
  -XX:+UseTLAB -Xcomp -XX:-BackgroundCompilation -XX:-DoEscapeAnalysis -server
  ```

  输出内容：
  ```
  Pass time: 98
  ```
  显式打开 TLAB；启用对所有函数的 JIT 以及禁止后台编译（为了控制环境变量）；禁用逃逸分析，防止栈上分配的行为影响测试效果；Server 模式下才支持逃逸分析参数 DoEscapeAnalysis。

  禁用 TLAB。
  VM options：
  ```
  -XX:-UseTLAB -Xcomp -XX:-BackgroundCompilation -XX:-DoEscapeAnalysis -server
  ```

  输出内容：
  ```
  Pass time: 241
  ```

  TLAB 是否启用，对对象分配的影响很大。 TLAB 占用的空间不大，很容易装满，大对象无法在其上边分配，而直接分配在堆空间。如果100KB的 TLAB 空间，已经使用了80KB，需要再分配30KB时，虚拟机有两个选择：1.废弃当前 TLAB，浪费20KB；2.30KB空间直接分配在堆空间，保留当前 TLAB，剩余空间装低于20KB的对象。当发生请求分配内存空间的对象大于 TLAB 可用空间，虚拟机会维护一个叫做 refill_waste 的值，对象占用空间大于该值时，在堆空间分配；小于该值，则废弃 TLAB，新建 TLAB 来分配对象。可以通过 TLABRefillWasteFraction 来调整，默认为64，使用约为1/64的 TLAB 空间作为 refill_waste。

  TLAB 和 refill_waste 都会在运行时不断调整，使系统的运行状态达到最优。禁用自动调整 TLAB 使用参数`-XX:-ResizeTLAB`,同时使用参数`-XX:TLABSize`指定 TLAB 大小。跟踪 TLAB 使用情况，需要添加参数`-XX:+PrintTLAB`。

  VM options：
  ```
  -XX:+UseTLAB -XX:+PrintTLAB -XX:+PrintGC -XX:TLABSize=102400 -XX:-ResizeTLAB -XX:TLABRefillWasteFraction=100 -XX:-DoEscapeAnalysis -server
  ```

  输出内容：
  ```
  TLAB: gc thread: 0x0000000027d9a000 [id: 29564] desired_size: 100KB slow allocs: 0  refill waste: 1024B alloc: 0.03800     5000KB refills: 1 waste 100.0% gc: 102360B slow: 0B fast: 0B
  TLAB: gc thread: 0x0000000027ccc800 [id: 28924] desired_size: 100KB slow allocs: 0  refill waste: 1024B alloc: 0.03800     5000KB refills: 1 waste 99.5% gc: 101912B slow: 0B fast: 0B
  TLAB: gc thread: 0x0000000002c93800 [id: 7124] desired_size: 100KB slow allocs: 16  refill waste: 1024B alloc: 0.03800     5000KB refills: 1312 waste  0.0% gc: 0B slow: 23544B fast: 8B
  TLAB totals: thrds: 3  refills: 1314 max: 1312 slow allocs: 16 max 16 waste:  0.2% gc: 204272B max: 102360B slow: 23544B max: 23544B fast: 8B max: 8B
  [GC (Allocation Failure)  131584K->775K(502784K), 0.0013260 secs]
  Pass time: 131
  ```
  输出内容分成两部分：首先是每一个线程的 TLAB 使用情况，其次是以 TLAB totals 为首的整体 TLAB 统计情况。desired_size 是 TLAB 的大小，通过参数`-XX:TLABSize=102400`指定100KB，slow allocs 表示上一次新生代 GC 到现在为止慢分配次数，慢分配是指由于 TLAB 空闲空间不能满足较大对象的分配，将对象直接分配在堆上。refill waste 表示 refill_waste 值。alloc 表示当前线程的 TLAB 分配比例（自上一次新生代 GC 后 number_of_refills * desired_size / used_tlab 的加权平均值）和使用评估量（加权平均值 * used_tlab（TLAB 上大约合计被分配了多少空间））。refills 表示该线程的 TLAB 空间被重新分配并填充的次数。

  waste 表示空间的浪费比例。浪费的空间由三部分组成：gc、slow 和 fast。gc 表示当前新生代 GC 发生时，空闲的 TLAB 空间；slow 和 fast 都表示 TLAB 被废弃时，没有被使用的 TLAB 空间，两者的不同处是 fast 表示这个 refill 操作是通过 JIT 编译优化的（禁用 JIT，fast 永远为0）。wast 比例是由浪费空间之和（gc + slow + fast）与总分配大小（_number_of_refills * _desired_size）的比值。 

  最后的 TLAB totals 则显示了所有线程的统计情况。thrds 相关线程数，refills 表示所有线程 refills 总数，之后的 max 表示 refills 次数最多的线程 refills 次数。

  对象分配流程：如果运行栈上分配，系统会先进行栈上分配；没有开启或者不符合栈上分配条件会进行 TLAB分配；TLAB 分配不成功，尝试堆上分配；满足直接进入老年代的条件（PretenureSizeThreshold 等参数），在老年代分配内存空间，否则在 Eden 区分配内存空间。如果有必要，会进行一次新生代 GC。
  ```mermaid
  graph TD
  A[尝试栈上分配] --成功--> B[栈上分配] 
  C[尝试 TLAB 分配] --成功--> D[TLAB 分配] 
  E[是否满足直接进入老年代的条件] --满足--> F[老年代分配] 
  G[Eden 区分配]
  A --失败--> C
  C --失败--> E
  E --不满足--> G
  ```

#### 5.5.6 方法 finalize() 对垃圾回收的影响
  Java 中提供了一个类似于 C++ 中析构函数的机制——finalize()方法。
  ```java
  protected void finalize() throws Throwable {}
  ```
  可以在子类中重载该方法，用于对象被回收时进行资源释放。不过不推荐使用：
  - finalize() 可能会导致对象复活
  - finalize() 执行时机没有保障，完全由 GC 线程决定，极端情况下不发生 GC，finalize() 就不会执行
  - 糟糕的 finalize() 会严重影响 GC 的性能

  finalize() 方法是由 FinalizerThread 线程处理，每一个即将被回收达并且包含 finalize() 方法的对象都会在回收之前加入 FinalizerThread 的执行队列——java.lang.ref.ReferenceQueue 引用队列，内部通过链表实现，队列每一项为 java.lang.ref.Finalizer 引用对象，其本质是一个引用。Finalizer 内部封装了实际的回收对象， next、prev 为了实现链表，分别指向队列当前元素的下一个和上一个元素，reference 字段指向实际的对象引用。

  对象在被回收之前，被 Finalizer 的 referent 字段进行“强引用”，并加入了 FinalizerThread 的执行队列，对象将变为可达，阻止对象的正常回收。在引用队列中的元素排队执行 finalizer() 方法，一旦出现性能问题，将导致垃圾对象长时间堆积在内存中，出现 OOM 异常。

  
  LongFinalize：[LongFinalize](../java/com/ibgdn/chapter_5/LongFinalize.java)

  VM options：
  ```
  -Xmx10m -Xms10m -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="D:/oom-5.dump"
  ```
  sleep() 方法模拟一个耗时操作，主方法不断产生新的 LF 对象，每次循环中产生的 LF 对象（占用大约512字节）都会在下一个循环中失效，因为局部变量作用域过期，对象没有其他引用，所有产生的 LF 对象都应该可以被回收。理论上10M堆空间完全可以满足，可能需要多进行几次 GC，但是为什么会出现 OOM 呢？系统中由大量的 Finalizer 类，FinalizerThread 执行队列可能一直持有对象而来不及执行，因此大量的对象堆积无法被释放，最终导致 OOM。

  去除重载的 finalizer() 方法，再次以相同的参数运行这段程序，将会很快正常结束，说明 finalizer() 对 GC 产生了影响。

  **注意：一个糟糕的 finalizer() 方法可能会使对象长时间被 Finalizer 引用，得不到释放的对象会进一步增加 GC 的压力。finalizer() 方法应该尽量少的使用。**

  某些场合，使用 finalizer() 方法会起到双保险的作用，比如 MySQL 的 JDBC 驱动中，com.mysql.jdbc.ConnectionImpl 实现 finalizer() 方法：
  ```java
  protected void finalizer() throws Throwable {
    cleanup(null);
    super.finalizer();
  }
  ```
  当一个 JDBC Connection 被回收时，需要关闭连接，即 cleanup() 方法。在回收之前，如果正常调用 Connection.close() 方法，连接就会被显示关闭，cleanup() 方法就不需要做什么。如果没有显示关闭，Connection 对象被回收，会隐式的进行连接的关闭，确保没有数据库连接的泄漏。官方积极鼓励开发过程中显示关闭数据库连接，finalizer() 只是一种正常方法出现意外的补偿措施，但是调用时间不确定，不能单独作为可靠的资源回收手段。