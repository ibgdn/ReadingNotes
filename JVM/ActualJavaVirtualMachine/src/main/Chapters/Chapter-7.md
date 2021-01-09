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