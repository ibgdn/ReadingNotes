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
  从这段日志中可以看到，ChildPR 子类确实己经被加载入系统，但是 ChildPR 的初始化却未进行。

  另外一个有趣的例子是使用引用常量。在前文介绍的几种主动使用的情况中，特别注明：“当使用类或接口的静态字段时（final 常量除外）”，也就是说引用 final 常量并不会引起类的初始化。

  [引用 final 常量并不会引起类的初始化](../java/com/ibgdn/chapter_10/UseFinalField.java)

  运行以上代码输出结果为：
  ```
    CONST
  ```
  FinalFieldClass 类并没有因为其常量字段 constString 被引用而初始化。这是因为在 Class 文件生成时，final 常量由于其不变性，做了适当的优化。

  在所有的类加载日志中，没有 FinalFieldClass 类出现，可见 javac 在编译时，将常量直接植入目标类，不再使用被引用类。

  注意：**并不是在代码中出现的类，就一定会被加载或者初始化。如果不符合主动使用的条件，类就不会初始化**。

#### 10.1.2 加载类
  加载类处于类装载的第一个阶段。在加载类时，Java 虚拟机必须完成以下工作：

  - 通过类的全名，获取类的二进制数据流。
  - 解析类的二进制数据流为方法区内的数据结构。
  - 创建 java.lang.Class 类的实例，表示该类型。

  对于类的二进制数据流，虚拟机可以通过多种途径产生或获得。最一般地，虚拟机可能通过文件系统读入一个 class 后缀的文件，或者也可能读入 JAR、ZIP 等归档数据包，提取类文件。除了这些形式外，任何形式都是可以的。比如，事先将类的二进制数据存放在数据库中，或者通过类似于 HTTP 之类的协议通过网络进行加载，甚至是在运行时生成一段 Class 的二进制信息。

  在获取到类的二进制信息后，Java 虚拟机就会处理这些数据，并最终转为一 java.lang.Class 的实例，java.lang.Class 实例是访问类型元数据的接口，也是实现反射的关键数据。通过 Class 类提供的接口，可以访问一个类型的方法、字段等信息。

  通过 Class 类，获得了 java.lang.String 类的所有方法信息，并打印方法访问标示符以及方法签名。
  ```java
    public static void main(String[] args) throws Exception {
        Class clzStr = Class.forName("java.lang.String");
        Method[] methods = clzStr.getDeclaredMethods();
        for (Method method : methods) {
            String mod = Modifier.toString(method.getModifiers());
            System.out.print(mod + " " + method.getName() + " (");
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 0) {
                System.out.println(")");
            }
            for (int i = 0; i < parameters.length; i++) {
                char end = i == parameters.length - 1 ? ')' : ',';
                System.out.print(parameters[i].getSimpleName() + end);
            }
            System.out.println();
        }
    }
  ```

  代码第2行，通过 Class.forName() 方法得到代表 String 类的 Class 实例。第3行通过 Class getDeclaredMethods() 方法取得类的所有方法列表。

  第5行取得方法的访问标示符，并通过 Modifier.toString() 方法将访问标示符转为可读字符串。第7行取得方法的所有参数。第11行输出方法的参数。

  这段代码运行后，部分输出结果如下：
  ```
    ...
    public indexOf (int,int)
    public indexOf (int)
    static indexOf (char[],int,int,char[],int,int,int)
    static indexOf (char[],int,int,String,int)
    public static valueOf (int)
    public static valueOf (long)
    public static valueOf (float)
    public static valueOf (boolean)
    public static valueOf (char[])
    public static valueOf (char[],int,int)
    ...
  ```