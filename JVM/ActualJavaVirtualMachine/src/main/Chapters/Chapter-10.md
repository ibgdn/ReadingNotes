### 10.1 来去都有序：看懂 Class 文件的装载流程
  Class 类型通常以文件的形式存在（当然，任何二进制流都可以是 Class 类型），只有被 Java 虚拟机装载的 Class 类型才能在程序中使用。系统装载 Class 类型可以分为加载、连接和初始化3个步骤。其中，连接又可分为验证、准备和解析3步。

#### 10.1.1 类装载的条件
  Class 只有在必须要使用的时候才会被装载，Java 虚拟机不会无条件地装载 Class 类型。Java 虚拟机规定，一个类或接口在初次使用前，必须要进行初始化。这里指的“使用”，是指主动使用，主动使用只有下列几种情况：

  - 当创建一个类的实例时，比如使用 new 关键字，或者通过反射、克隆、反序列化。
  - 当调用类的静态方法时，即当使用了字节码 invokestatic 指令。
  - 当使用类或接口的静态字段时（final 常量除外），比如，使用 getstatic 或者 putstatic 指令。
  - 当使用 java.lang.reflect 包中的方法反射类的方法时。
  - 当初始化子类时，要求先初始化父类。
  - 作为启动虚拟机，含有 main() 方法的那个类。

  除了以上的情况属于主动使用，其他的情况均属于被动使用。被动使用不会引起类的初始化。

  下面先来看一个主动引用的例子
  [主动引用](../java/com/ibgdn/chapter_10/ActiveReference.java)

  以上代码申明了 3个类，ParentAR、ChildAR 和 ActiveReference， ChildAR 为 ParentAR 的子类。若 ParentAR 被初始化，根据代码中的 static 语句块可知，将会打印“ParentAR init.”，若 ChildAR 被初始化，则会打印“ChildAR init.”。执行结果为：
  ```
    ParentAR init.
    ChildAR init.
  ```
  由此可知，系统首先装载 ParentPR 类，接着再装载 ChildPR 类。符合主动装载中的两个条件，使用 new 关键字创建类的实例会装载相关类，以及在初始化子类时，必须先初始化父类。

  被动引用的例子。被动引用不会导致类的装载。
  [被动引用](../java/com/ibgdn/chapter_10/PassiveReference.java)

  査看以上代码，ParentPR 中有静态变量 v，并且在 PassiveReference 中，使用其子类 ChildPR 去调用父类中的变量。运行以上代码，输出结果如下：
  ```
    ParentPR init.
    100
  ```
  可以看到，虽然在 PassiveReference 中，直接访问了子类对象，但是 ChildPR 子类并未被初始化，只有 ParentPR 父类被初始化。可见，在引用一个字段时，只有直接定义该字段的类，才会被初始化。

  注意：**虽然 ChildPR 类没有被初始化，但是，此时 ChildPR 类已经被系统加栽，只是没有进入到初始化阶段**。

  如果使用`-XX:+TraceClassLoading`参数运行这段代码，就会得到以下日志（限于篇幅，只列出部分输出）：
  ```
    [Loaded com.ibgdn.chapter_10.ParentPR from file:/ReadingNotes/JVM/ActualJavaVirtualMachine/target/classes/]
    [Loaded com.ibgdn.chapter_10.ChildPR from file:/ReadingNotes/JVM/ActualJavaVirtualMachine/target/classes/]
    ParentPR init.
    100
    [Loaded java.lang.Shutdown from \jdk1.8.0_231\jre\lib\rt.jar]
    [Loaded java.lang.Shutdown$Lock from \jdk1.8.0_231\jre\lib\rt.jar]
    [Loaded java.net.URI from \jdk1.8.0_231\jre\lib\rt.jar]
  ```
  由此可知，系统首先装载 Parent 类，接着再装载 Child 类。符合主动装载中的两个条件，使用 new 关键字创建类的实例会装载相关类，以及在初始化子类时，必须先初始化父类。