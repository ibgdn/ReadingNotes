### 8.1 完全就是锁存在的理由：锁的基本概念和实现
  锁是多线程软件开发的必要工具之一，它的基本作用是保护临界区资源不会被多个线程同时访问而受到破坏。如果由于多线程访问造成对象数据的不一致，那么系统运行将会得到错误的结果。通过锁，可以让多个线程排队，一个一个地进入临界区访问目标对象，使目标对象的状态总是保持一致，这也就是锁存在的价值。

#### 8.1.1 理解线程安全
  通过锁，可以实现线程安全。对于线程安全，简单的理解就是，在多线程环境下，无论多个线程如何访问目标对象，目标对象的状态应该始终是保持一致的，线程的行为也总是正确的。

  简单示例说明线程安全：

  线程 A 和线程 B 在数据库中分别读入两条学生成绩记录，线程 A 读入小明98分，线程 B 读入小王考77分。现在需要将从数据库里得到的数据保存到对象实例 S 上，在进行其他相应的业务逻辑处理。此时，对象实例 S 就是临界区资源。如果没有锁对 S 进行保护，任由两个线程随意处理，由于线程间的无序性访问，一种可能的访问结果是：线程 A 将学生名“小明”赋予对象 S，接着线程 B 将学生名“小王”赋予对象 S，覆盖线程 A 的操作。然后，线程 B 将成绩77分赋予对象 S，最后线程 A 将成绩98分赋予对象 S，覆盖了线程 B 的操作。这一组操作得到的结果是，对象 S 中保存了部分小明的数据（成绩），部分小王的数据（学生名），显然这样一个对象是没有任何意义的，也就是对象处于一种不一致的状态，这也正是线程不安全导致的恶果。

  要处理这个问题就可以使用锁来解决。对于对象 S 的所有操作使用锁进行控制，每一次只允许一个线程对其操作，如果线程 A 先获得锁，那么线程 A 将完成它对对象 S 的所有处理，最后释放锁。而线程 B 由于没能请求到锁，就会进行等待，直到线程 A 释放了锁，线程 B 才得以获得锁。在这种情况下，只有在被锁保护的代码段内，对象的状态会出现短暂的不一致（幸运的是，这种状态被锁保护，因此其他线程也无法观察到这种状态），但只要线程 A 或者线程 B 完成了它的工作，对象 S 的状态就是一致的，即对象 S 保存的数据不是小明的，就是小王的，而不是两者的混合体。

  数据的不一致不仅会使得程序给出错误的结果，也可能导致程序异常崩溃。

  [ArrayList 在多线程下使用](../java/com/ibgdn/chapter_8/ThreadUnSafe.java)

  两个线程同时向 List 集合中增加数据，由于 ArrayList 不是线程安全的，很可能抛出如下错误（也有可能不出错）。

  输出结果：
  ```
  Exception in thread "Thread-1" java.lang.ArrayIndexOutOfBoundsException: 10
	  at java.util.ArrayList.add(ArrayList.java:463)
	  at com.ibgdn.chapter_8.ThreadUnSafe$AddToList.run(ThreadUnSafe.java:35)
	  at java.lang.Thread.run(Thread.java:748)
  ```

  出现这个问题，是因为两个线程同时对 ArrayList 进行写操作，破坏了 ArrayList 内部数据的一致性，导致其中一个线程访问了错误的数组索引。简单的修正方法是使用 Vector 代替 ArrayList，Vector 通过内部锁实现对 List 对象控制。

#### 8.1.2 对象头和锁
  Java 虚拟机的实现中，每个对象都有一个对象头，用于保存对象的系统信息。对象头中有一个称为 Mark Word 的部分，是实现锁的关键。Mark Word 在32位系统中占32位数据，64位系统中占64位数据。是一个多功能的数据区，可以存放对象的哈希值、对象年龄、锁的指针（是否占有锁，占有哪个锁）等信息。

  32位系统，普通对象的对象头：
  ```
  hash:25 ------------>| age:4  biased_lock:1   lock:2
  ```
  25位比特表示对象的哈希值，4位比特表示对象的年龄，1位比特表示是否为偏向锁，2位比特表示锁的信息。

  偏向锁对象格式如下：
  ```
  [JavaThread*  |   epoch   |   age |   1   |   01]
  ```
  前23位比特表示持有偏向锁的线程，后续2位比特表示偏向锁的时间戳（epoch），4位比特表示对象年龄，年龄后1位比特固定为1，表示偏向锁，最后2位为01表示可偏向/未锁定。

  当对象处于轻量级锁锁定时，Mark Word 如下（00表示最后两位的值）：
  ```
  [ptr  |   00] locked
  ```
  此时，它指向存放在获得锁的线程栈中的该对象真实对象头。

  当对象处于重量级锁定时，Mark Word 如下：
  ```
  [ptr  |   01] monitor
  ```
  此时，最后2位为10，整个 Mark Word 表示指向 Monitor 的指针。

  对象处于普通的未锁定状态，格式如下：
  ```
  [header   |   0   |   01] unlocked
  ```
  前29位表示对象的哈希值、年龄等信息。倒数第3位为0，最后两位01，表示未锁定。可以发现，最后两位的值和偏向状态时是一样的，此时，虚拟机正是通过倒数第3位比特来区分是否是偏向锁。

### 8.2 避免残酷的竞争：锁在 Java 虚拟机中的实现和优化
  在多线程程序中，线程之间的竞争是不可避免的，而且是一种常态，如何使用更高的效率处理多线程的竞争，是 Java 虚拟机一项重要的使命。

#### 8.2.1 偏向锁
  偏向锁是JDK1.6提出的一种锁优化的方式。其核心思想是，如果程序没有竞争，则取消之前已经取得锁的线程同步操作。也就是说，若某一锁被线程获取后，便进入偏向模式；当线程再次请求这个锁时，无需再进行相关的同步操作，从而节省了操作时间；如果在此之间有其他线程进行了锁请求，则锁退出偏向模式。`-XX:+UseBiasedLocking`可以设置启用偏向锁。

  当锁对象处于偏向模式时，对象头会记录获得锁的线程：
  ```
  [JavaThread*  |   epoch   |   age |   1   |   01]
  ```

  [偏向锁](../java/com/ibgdn/chapter_8/Biased.java)
  输出结果：
  ```
  686
  ```
  使用一个线程对 Vector 进行写入操作，由于对 Vector 的访问，其内部都是用同步锁控制，故每次 add 操作都会请求 numberList 对象的锁。

  VM options：
  ```
  -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 -client -Xmx512m -Xms512m
  ```

  输出结果：
  ```
  319
  ```
  `-XX:BiasedLockingStartupDelay`表示虚拟机在启动后，立即启用偏向锁。如果不设置该参数，虚拟机默认会在启动4秒后，才启用偏向锁，考虑到程序运行时间较短，故做此设置。

  **偏向锁在锁竞争的激烈的场合没有太强的优化效果**。因为大量的竞争会导致持有锁的线程不停地切换，锁也很难一直保持在偏向模式，此时，使用锁偏向不仅得不到性能的优化，反而有可能降低系统性能。在激烈竞争的场合，可以尝试使用`-XX:-UseBasedLocking`参数禁用偏向锁。

#### 8.2.2 轻量级锁
  如果偏向锁失败，Java 虚拟机会让线程申请轻量级锁。轻量级锁在虚拟机内部，使用一个称为 BasicObjectLock 的对象实现，这个对象内部由一个 BasicLock 对象和一个持有该锁的 Java 对象指针组成。BasicObjectLock 对象放置在 Java 栈的栈帧中。在 BasicLock 对象内部还维护着 displaced_header 字段，它用于备份对象头部的 Mark Word。

  一个线程持有一个对象的锁时，对象头部 Mark Word：
  ```
  [ptr  | 00] locked
  ```

  末尾两位比特为00，整个 Mark Word 为指向 BasicLock 对象的指针。由于 BasicObjectLock 对象在线程栈中，因此该指针必然指向持有该锁的线程栈空间。当需要判断某一线程是否持有该对象锁时，也只需简单地判断对象头的指针是否在当前线程的栈地址范围内即可。同时，BasicLock 对象的 displaced_header 字段，备份了原对象的 Mark Word 内容。BasicObjectLock 对象的 obj 字段则指向该对象。

  轻量级锁核心代码
  ```java
  markOop mark = obj -> mark();
  lock -> set_displaced_header(mark);
  if (mark == (markOop) Atomic::cmpxchg_ptr(lock, obj() -> mark_addr(), mark)) {
    TEVENT (slow_enter: release stacklock);
    return ;
  }
  ```

  首先，BasicLock 通过 set_displaced_header() 方法备份了原对象的 Mark Word。接着，使用 CAS 操作，尝试将 BasicLock 的地址复制到对象头的 Mark Word。如果复制成功，那么加锁成功，否则认为加锁失败。如果加锁失败，那么轻量级锁就有可能被膨胀为重量级锁。

#### 8.2.3 锁膨胀
  当轻量级锁失败，虚拟机就会使用重量级锁，同时 Mark Word 为：
  ```
  [ptr  | 10] monitor
  ```

  末尾2比特标记位被置为10。整个 Mark Word 表示指向 monitor 对象的指针。在轻量级锁处理失败后，虚拟机会执行如下操作：
  ```java
  lock -> set_displaced_header(markOopDesc::unused_mark());
  ObjectSynchronizer::inflate(THREAD, obj()) -> enter(THREAD);
  ```

  首先，废弃前面 BasicLock 备份的对象头信息。然后，正式启用重量级锁，启用过程分两步：通过`inflate()`进行锁膨胀，以获得对象的 ObjectMonitor；然后使用`enter()`方法尝试进入该锁。

  在`enter()`方法调用时，线程很可能会在操作系统层面被挂起，线程间切换和调度的成本就会比较高。

#### 8.2.4 自旋锁

  锁膨胀后，进入 ObjectMonitor 的`enter()`方法，线程很可能会在操作系统层面被挂起，线程上下文切换的性能损失就比较大。因此，在锁膨胀之后，虚拟机做最后的争取，希望线程可以尽快进入临界区而避免被操作系统挂起。一种较为有效的手段就是使用自旋锁。

  自旋锁可以使线程在没有取得锁时，不被挂起，转而去执行一个空循环（即所谓的自旋），在若干个空循环后，线程如果可以获得锁，则继续执行后续流程；如果线程依然不能获得锁，才会被挂起。

  使用自旋锁后，线程被挂起的几率相对减少，线程执行的连贯性相对加强。因此，对于那些锁竞争不是很激烈，锁占用时间很短的并发线程，具有一定的积极意义；但对于锁竞争激烈，单线程锁占用时间长的并发程序，自旋锁在自旋等待后，往往依然无法获得对应的锁，不仅白白浪费了 CPU 时间，最终还是免不了执行被挂起的操作，反而浪费了系统资源。

  JDK1.6中，Java 虚拟机提供`-XX:+UseSpinning`参数来开启自旋锁，使用`-XX:PreBlockSpin`参数来设置自旋锁的等待次数。

  JDK1.7中，自旋锁的参数被取消，虚拟机不再支持由用户配置自旋锁。自旋锁总是会执行，自旋次数也由虚拟机自行调整。

#### 8.2.5 锁消除
  锁消除是 Java 虚拟机在 JIT 编译时，通过对运行上下文的扫描，去除不可能存在共享资源竞争的锁。通过锁消除，可以节省毫无意义的请求锁时间。

  如果不可能存在竞争，为什么程序员还要加上锁呢？在 Java 软件开发过程中，开发人员必然会使用一些 JDK 的内置 API，比如 StringBuffer、Vector 等。这些常用的工具类可能会被大面积地使用，虽然这些工具类本身可能有对应的非线程安全版本，但是开发人员也很有可能在完全没有多线程竞争的场合使用它们。

  在这种情况下，工具类内部的同步方法就是不必要的。虚拟机可以在运行时，基于逃逸分析技术，捕获到这些不可能存在竞争却有申请锁的代码段，并消除这些不必要的锁，从而提高系统性能。

  [变量仅作用于方法体内部](../java/com/ibgdn/chapter_8/LockEliminate.java)

  输出结果：
  ```
  createStringBuffer: 197 ms
  ```

  逃逸分析和锁消除分别可以使用参数`-XX:+DoEscapeAnalysis`和`-XX:+EliminateLocks`开启（锁消除必须工作在`-server`模式下）。

  关闭锁消除，每次`append()`操作都会进行锁的申请

  VM options：
  ```
  -server -XX:+DoEscapeAnalysis -XX:-EliminateLocks -Xcomp -XX:-BackgroundCompilation -XX:BiasedLockingStartupDelay=0
  ```

  输出结果：
  ```
  createStringBuffer: 232 ms
  ```

  VM options：
  ```
  -server -XX:+DoEscapeAnalysis -XX:+EliminateLocks -Xcomp -XX:-BackgroundCompilation -XX:BiasedLockingStartupDelay=0
  ```

  输出结果：
  ```
  createStringBuffer: 218 ms
  ```

  使用锁消除后，性能有了较为明显的改善。偏向锁本身简化了锁的获取，其性能较好。本例中使用```-XX:BiasedLockingStartupDelay```参数迫使偏向锁在启动的时候就生效，即便如此，性能也不如锁消除后的代码。

### 8.3 应对残酷的竞争：锁在应用层的优化思路
#### 8.3.1 减少锁持有时间
  对于使用锁进行并发控制的应用程序而言，在锁竞争过程中，单个线程对锁的持有时间与系统性能有着直接的关系。如果线程持有锁的时间很长，锁的竞争也就越激烈。程序开发过程中，应该尽可能地减少对某个锁的占有时间，减少线程间的互斥几率。
  
  ```java
  public synchronized void syncMethod(){
    otherCode1();
    mutextMethod();
    otherCode2();
  }
  ```
  `syncMethod()`方法中，如果只有`mutextMethod()`方法有同步需求，而`otherCode1()`和`otherCode2()`并不需要做同步控制。如果`otherCode1()`和`otherCode2()`都是重量级方法，则会花费较长的 CPU 时间。如果同时并发量较大，使用这种对整个方法做同步的方案，会导致等待线程大量增加。因为一个线程，在进入该方法时获得内部锁，只有在所有任务都执行完成后，才会释放锁。

  较好的优化方案是，只在必要时进行同步，以减少线程持有锁的时间，提高系统的吞吐量。
  ```java
  public void syncMethod2(){
        otherCode1();
        synchronized (this) {
            mutextMethod();
        }
        otherCode2();
  }
  ```
  改进的代码中，只对`mutextMethod()`方法做了同步，锁占用时间相对较短，可以提高并行度。JDK 源码中常有类似代码，比如正则表达式的 Pattern 类：
  ```java
  public Mather matcher(CharSequence input) {
        if (!compiled) {
            synchronized (this) {
                if (!compiled) {
                    compile();
                }
            }
        }
        Matcher matcher = new Matcher(this, input);
        return matcher;
  }
  ```

  注意：**减少锁的持有时间，有助于降低锁冲突的可能性，进而提升系统的并发能力**

#### 8.3.2 减少锁的粒度
  减少锁的粒度也是一种削弱多线程锁竞争的有效手段。这种技术典型的使用场景是 ConcurrentHashMap 类的实现。普通集合对象的多线程同步，最常用的方式就是对`get()`和`add()`方法进行同步，每当对集合进行 get 或 add 操作时，总是获得集合对象的锁。因此，事实上没有两个线程可以做到真正的并发，任何线程在执行这些同步方法时，总要等待前一个线程执行完毕。在高并发时，激烈的锁竞争会影响系统的吞吐量。

  ConcurrentHashMap 将整个 HashMap 分成若干个段（Segment），每个段都是一个子 HashMap。如果需要在 ConcurrentHashMap 中增加一个新的表项，并不是将整个 HashMap 加锁，而是首先根据 hashcode 得到该表项应该被存放到的段（Segment）中，然后对该段加锁，并完成 put 操作。在多线程环境中，如果多个线程同时进行 put 操作，只要被加入的表项不存放在同一个段中，则线程间便可以做到真正的并行。

  默认情况下，ConcurrentHashMap 拥有16个段，如果幸运的话，ConcurrentHashMap 可以同时接受16个线程同时插入（同时插入不同段的情况），大大提高吞吐量。如下图所示6个线程同时对 ConcurrentHashMap 进行访问，线程1、2、3分别访问受独立锁保护的段1、2、3，而线程4、5、6也需要访问段1、2、3，则必须等待前面的线程结束访问才能进入 ConcurrentHashMap。
  ```mermaid
  graph LR
  a[Thread4  Thread1] --> d[segment1]
  b[Thread5  Thread2] --> e[segment2]
  c[Thread6  Thread3] --> f[segment3]
  ```

  减少粒度会引入一个新问题：当系统需要取得全局锁时，消耗的资源会比较多。仍然以 ConcurrentHashMap 类为例，虽然`put()`方法很好地分离了锁，当试图访问 ConcurrentHashMap 全局信息时，就会需要同时取得所有段的锁方能顺利实施。比如 ConcurrentHashMap 的`size()`方法，返回 ConcurrentHashMap 的有效表项数量之和。要获取这个信息，需要取得所有子段的锁，`size()`方法部分代码如下：
  ```java
  sum = 0;
  // 对所有的段加锁
  for (int i = 0; i < segments.length; ++i)
      segments[i].lock();
  // 统计总数
  for (int i = 0; i < segments.length; ++i)
      sum += segments[i].count;
  // 释放所有的锁
  for (int i = 0; i < segments.length; ++i)
      segments[i].unlock();
  ```
  计算所有有效表的数量，要先获得所有段的锁，然后再求和。ConcurrentHashMap 的`size()`方法并不总是这样执行。事实上，`size()`方法会先使用无锁的方式求和，如果失败才会尝试使用加锁的方法。在高并发场景下，ConcurrentHashMap 的`size()`方法的性能依然要差于同步的 HashMap。

  注意：**所谓减少锁粒度，就是指缩小锁定对象的范围，从而减少锁冲突的可能性，进而提高系统的并发能力**

#### 8.3.3 锁分离
  锁分离是减小锁粒度的一个特例，依据应用程序的功能特点，将一个独占锁分成多个锁。

  锁分离的典型示例是`java.util.concurrent.LinkedBlockingQueue`的实现。`take()`和`put()`方法分别实现了从队列中取得数据和往队列中增加数据的功能。LinkedBlockingQueue 是基于链表的数据结构，两个方法的操作分别作用于队列的前端（take）和尾部（put），理论上两者并不冲突。

  如果使用独占锁，则需要在两个操作进行时，获取当前队列的独占锁，`take()`和`put()`方法就不可能真正并发进行，运行时需要彼此等待对方释放锁资源，锁竞争会比较激烈，从而影响程序在高并发时的性能。

  JDK 中的加了两把不同的锁
  ```java
    /** Lock held by take, poll, etc */
    // take() 方法需要持有 takeLock
    private final ReentrantLock takeLock = new ReentrantLock();
  
    /** Wait queue for waiting takes */
    private final Condition notEmpty = takeLock.newCondition();
  
    /** Lock held by put, offer, etc */
    // put() 方法需要持有 putLock
    private final ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting puts */
    private final Condition notFull = putLock.newCondition();
  ```
  以上代码段定义了 takeLock 和 putLock，分别在`take()`和`put()`方法操作中使用，两个方法就相互独立，不会存在竞争关系。存在的竞争只是同名方法`take()`和`take()`的竞争。

  `take()`方法实现如下：
  ```java
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        // 不能有两个线程同时取数据
        takeLock.lockInterruptibly();
        try {
            // 如果当前没有可用数据，就会一直等待
            while (count.get() == 0) {
                // 等待，put() 操作的通知
                notEmpty.await();
            }
            // 取得第一个数据
            x = dequeue();
            // 数量减1，原子操作，因为会和 put() 方法同时访问 count。
            // 注意：变量 c 是 count 减 1 的值
            c = count.getAndDecrement();
            if (c > 1)
                // 通知其他 take() 方法操作
                notEmpty.signal();
        } finally {
            // 释放锁
            takeLock.unlock();
        }
        if (c == capacity)
            // 通知 put() 方法，已有空余空间
            signalNotFull();
        return x;
    }
  ```

  `put()`方法实现如下
  ```java
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        // 不能有两个线程同时存数据
        putLock.lockInterruptibly();
        try {
            // 如果队列已满
            while (count.get() == capacity) {
                // 等待
                notFull.await();
            }
            // 将数据放入最后一项
            enqueue(node);
            // 更新总数，变量 c 是 count 加1前的值
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                // 有足够的空间，通知其他线程
                notFull.signal();
        } finally {
            // 释放锁
            putLock.unlock();
        }
        if (c == 0)
            // 插入成功后，通知 take() 方法取数据
            signalNotEmpty();
    }
  ```
  通过 takeLock 和 putLock 两把锁，LinkedBlockingQueue 实现了取数据和写数据的分离，使两者真正意义上成为可并发的操作。

#### 8.3.4 锁粗化
  通常情况下，为了保证多线程间的有效并发，会要求每个线程持有锁的时间尽量短，在使用完公共资源后，应该立即释放锁。等待在这个锁上的其他线程才能尽早的获得资源执行任务。但是，如果对同一个锁不停地进行请求、同步和释放，其本身也会消耗系统宝贵的资源，反而不利于性能的优化。

  虚拟机在遇到一连串连续地对同一锁不断进行请求和释放的操作时，便会把所有的锁操作整合成对锁的一次请求，从而减少对锁的请求同步次数。这个操作叫做锁的粗化。比如代码段：
  ```java
    public void demoMethod () {
        synchronized (lock) {
            // do something.
        }
        
        // 做其他不需要同步的工作，但能很快执行完毕
        synchronized (lock) {
            // do something.
        }
    }
  ```
  会被整合成如下形式：
  ```java
    public void demoMethod () {
        // 整合成一次锁请求
        synchronized (lock) {
            // do something.
            // 做其他不需要同步的工作，但能很快执行完毕
            // do something.
        }
    }
  ```

  循环内请求锁的例子：
  ```java
      for (int i = 0; i < CIRCLE; i++) {
        synchronized (lock) {
            
        }
      }
  ```
  以上代码在每一次循环时，都对同一个对象申请锁，对锁进行大量的请求。此时，应该将锁粗化成（只进行一次锁请求）：
  ```java
    synchronized (lock) {
      for (int i = 0; i < CIRCLE; i++) {

      }
    }
  ```

  注意：**性能优化就是根据运行时的真实情况，对各个资源点进行权衡折衷的过程。锁粗化的思想和减少锁持有时间是相反的，但在不同场合，它们的效果并不相同**

### 8.4 无招胜有招：无锁
  为了确保程序和数据的线程安全，使用“锁”是最直观的一种方式。但是在高并发场景下，对“锁”的激烈竞争可能会成为系统瓶颈。

#### 8.4.1 理解 CAS
  基于锁的同步方式，是一种阻塞的线程间同步方式，无论使用信号量，重入锁或者内部锁，受到核心资源的限制，不同线程间在锁竞争时，总不能避免相互等待，从而阻塞当前线程。为了避免这个问题，非阻塞同步的方式就被提出，最简单的一种非阻塞同步就以 ThreadLocal 为代表，每个线程拥有各自独立的变量副本，并行计算时，无需相互等待。

  本节将介绍一种更为重要的，基于比较并交换（Compare And Swap）CAS 算法的无锁并发控制方法。

  与锁的实现相比，无锁算法的设计和实现都要复杂得多，但由于其非阻塞性，它对死锁问题天生免疫，并且，线程间的相互影响也远远比基于锁的方式要小。更为重要的是，使用无锁的方式完全没有锁竞争带来的系统开销，也没有线程间频繁调度带来的开销，因此，它要比基于锁的方式拥有更优越的性能。

  CAS 算法的过程是这样：它包含3个参数`CAS(V, E, N)`。 V 表示要更新的变量，E 表示预期值，N 表示新值。仅当 V 值等于 E 值时，才会将 V 的值设为 N，如果 V 值和 E 值不同，则说明己经有其他线程做了更新，则当前线程什么都不做。最后，CAS 返回当前 V 的真实值。CAS 操作是抱着乐观的态度进行的，它总是认为自己可以成功完成操作。当多个线程同时使用 CAS 操作一个变量时，只有一个会胜出并成功更新，其余均会失败。失败的线程不会被挂起，仅是被告知失败，并且允许再次尝试，当然也允许失败的线程放弃操作。基于这样的原理，CAS 操作即使没有锁，也可以发现其他线程对当前线程的干扰，并进行恰当的处理。

  在硬件层面，大部分的现代处理器都已经支持原子化的 CAS 指令。在JDK5.0以后，虚拟机便可以使用这个指令来实现并发操作和并发数据结构。并且，这种操作在虚拟机中可以说是无处不在。

  轻量级锁中展示的代码段就是使用 CAS 操作将锁地址复制到对象头的 Mark Word 中。
  ```java
    if (mark == (markOop) Atomic::cmpxchg_ptr(lock, obj() -> mark_addr(), mark)) {
        // do something.
    }
  ```

#### 8.4.2 原子操作
  为了能让 CAS 操作被 Java 应用程序充分使用，在 JDK 的`java.util.concurrent.atomic`包下，有一组使用无锁算法实现的原子操作类，主要有 AtomicInteger、AtomicIntegerArray、AtomicLong、AtomicLongArray 和 AtomicReference 等。它们分别封装了对整数、整数数组、长整型、长整型数组和普通对象的多线程安全操作。

  以 AtomicInteger 为例.它的核心方法有：
  ```java
    // 取得当前值
    public final int get();
    // 设置当前值
    public final void set(int newValue);
    // 设置新值，并返回旧值
    public final int getAndSet(int newValue);
    // 如果当前值为 expect，则设置为 u
    public final boolean compareAndSet(int expect, int u);
    // 当前值加1，返回旧值
    public final int getAndIncrement();
    // 当前值减1，返回旧值
    public final int getAndDecrement();
    // 当前值加 delta，返回旧值
    public final int getAndAdd(int delta);
    // 当前值加1，返回新值
    public final int incrementAndGet();
    // 当前值减1，返回新值
    public final int decrementAndGet();
    // 当前值加 delta，返回新值
    public final int addAndGet(int delta);
  ```

  以`getAndSet()`方法为例，看一下 CAS 算法是如何工作的：
  ```java
    public final int getAndSet (int newValue) {
        for (;;) {
            int current = get();
            if (compareAndSet(current, newValue)){
                return current;
            }
        }
    }
  ```
  在 CAS 算法中，首先是一个无穷循环，在这里，这个无穷循环用于多线程间的冲突处理，即在当前线程受其他线程影响而更新失败时，会不停地尝试，直到成功。

  方法`get()`用于取得当前值，并使用`compareAndSet()`方法进行更新，如果未受其他线程影响，则预期值就等于 current。因此，可以将值更新为 newValue，若更新成功，则退出循环。
  
  如果受其他线程影响，则在`compareAndSet()`方法执行时，预期值就不等于 current ，更新失败，则进行下一次循环，尝试继续更新，直到成功。

  因此，在整个更新过程中，无需加锁，无需等待。从这段代码中也可以看到，无锁的操作实际上将多线程并发的冲突处理交由应用层自行解决，这不仅提升了系统性能，还增加了系统的灵活性。但相对的，算法及编码的复杂度也明显地增加了。

#### 8.4.3 新宠儿 LongAdder
  无锁的原子类操作使用系统的 CAS 指令，有着远远超越锁的性能。在JDK1.8中引入 LongAdder 类，在`java.util.concurrent.atomic`包下，也使用了 CAS 指令。

  AtomicInteger 等原子类的实现机制，它们都是在一个死循环内，不断尝试修改目标值，直到修改成功。如果竞争不激烈，那么修改成功的概率就很高，否则，修改失败的概率就很高，在大量修改失败时，这些原子操作就会进行多次循环尝试，因此性能就会受到影响。

  结合前文介绍的减小锁粒度与 ConcurrentHashMap 的实现，可以想到一种对传统 AtomicInteger 等原子类的改进思路。虽然在 CAS 操作中没有锁，但是像减小锁粒度这种分离热点的思想依然可以使用。一种可行的方案就是仿造 ConcurrentHashMap，将热点数据分离。比如，可以将 AtomicInteger 的内部核心数据 value 分离成一个数组，每个线程访问时，通过哈希等算法映射到其中一个数字进行计数，而最终的计数结果，则为这个数组的求和累加。其中，热点数据 value 被分离成多个单元 cell，每个 cell 独自维护内部的值，当前对象的实际值由所有的 cell 累计合成，这样，热点就进行了有效的分离，提高了并行度。LongAdder 正是使用了这种思想。

  [LongAdder 累加计数](../java/com/ibgdn/chapter_8/LongAdderCount.java)
  ```
  LongAdder spend: 255ms v = 10000002
  LongAdder spend: 255ms v = 10000002
  LongAdder spend: 255ms v = 10000002
  ```
  锁操作耗时在`1647ms`左右，原子操作耗时约`718ms`，LongAdder 仅需`255ms`。