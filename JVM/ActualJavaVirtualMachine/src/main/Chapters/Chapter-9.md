## 9.2 虚拟机的基石：Class 文件
  在 Java 虚拟机规范中，Class 文件使用一种类似于语言结构体的方式进行描述，并且统一使用无符号整数作为其基本数据类型，由u1、u2、u4、u8分别表示无符号单字节、2字节、4字节和8字节整数。对于字符串，则使用u1数组进行表示。

  根据 Java 虚拟机规范的定义，一个 Class 文件可以非常严谨地被描述成：
  ```java
    ClassFile {
        u4              magic;
        u2              minor_version;
        u2              major_version;
        u2              constant_pool_count;
        cp_info         constant_pool[constant_pool_count-1];
        u2              access_flags;
        u2              this_class;
        u2              super_class;
        u2              interface_count;
        field_info      fields[fields_count];
        u2              methods_count;
        methods_info    methods[methods_count];
        u2              attributes_count;
        attribute_info  attributes[attributes_count];
    }
  ```
  Class 文件的结构严格按照该结构体的定义：
  - 文件以一个4字节的 Magic（被称为魔数）开头，紧跟着两个大小版本号。 
  - 在版本号之后是常量池，常量池的个数为 constant_pool_count，常量池中的表项有constant_pool_count-1项。
  - 常量池之后是类的访问修饰符、代表自身类的引用、父类引用以及接口数量和实现的接口引用。
  - 在接口之后，有着字段的数量和字段描述、方法数量以及方法的描述。
  - 最后，存放着类文件的属性信息。

#### 9.2.1 Class 文件的标志——魔数
  魔数（Magic Number）作为 Class 文件的标志，用来告诉 Java 虚拟机，这是一个 Class 文件。魔数是一个4个字节的无符号整数，它固定为`0xCAFEBABE`。众所周知，Java 的名字和咖啡有着不解之缘。当你看到 CAFEBABE 时，会想到什么？像不像 Cafe Baby （咖啡宝贝）呢？因此，Java 的开发者们就用了这个整数来表示 Class 文件。

  【示例9-1】如果一个 Class 文件不以`0xCAFEBABE`开头，虚拟机在进行文件校验的时候就会直接抛出以下错误：
  ```java
    Exception in thread "main" java.lang.ClassFormatError: Incompatible magic value 184466110 in class file ibgdn/ClassFile
  ```

  注意：**魔数表示 Class 文件的标示符，在程序开发时，软件设计者喜欢使用一些特殊的数字来表示固定的文件类型或者特殊的含义。除了 Java 文件以外，常用的 TAR 文件、PE 文件，甚至是网络 DHCP 报文内部，都会有类似的设计手法**。

  【示例9-2】下面以一段简单的代码来展示魔数的内容。
  [展示魔数的内容](../java/com/ibgdn/chapter_9/MagicNumber.java)

  使用软件 WinHex 打开以上代码生成的 Class 文件，可以很容易看到魔数。