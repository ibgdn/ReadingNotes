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

