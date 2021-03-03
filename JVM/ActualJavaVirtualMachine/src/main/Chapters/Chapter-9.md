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

#### 9.2.2 Class 文件的版本
  在魔数后面，紧跟着 Class 的小版本和大版本号。这表示当前 Class 文件，是由哪个版本的编译器编译产生的。首先出现的是小版本号，是一个两个字节的无符号整数，在此之后为大版本号，也用两个字节表示。

  Class 文件版本号和平台的对应
  大版本（十进制）|小版本|编译器版本
  :--:|:--:|:--:
  45  |3  |1.1
  46  |0  |1.2
  47  |0  |1.3
  48  |0  |1.4
  49  |0  |1.5
  50  |0  |1.6
  51  |0  |1.7
  52  |0  |1.8

  大版本号为`0x34`，换算为16进制为52，因此可以判断该 Class 文件是由JDK1.7的编译器，或者在JDK1.8下，使用`-target1.7`参数编译生成的。

  目前，高版本的 Java 虚拟机可以执行由低版本编译器生成的 Class 文件，但是低版本的 Java 虚拟机不能执行由髙版本编译器生成的文件。

  使用1.7的虚拟机，试图执行由1.8编译器产生的 Class 文件所导致的错误：
  ```java
        Exception in thread "main" java.lang.UnsupportedClassVersionError: hsdb/Main: Unsupported major.minor version 52.0
  ```

#### 9.2.3 存放所有常量——常量池
  常量池是 Class 文件中内容最为丰富的区域之一。随着 Java 虚拟机的不断发展，常量池的内容也日渐丰富。同时，常量池对于 Class 文件中的字段和方法解析也有着至关重要的作用，可以说，常量池是整个 Class 文件的基石。在版本号之后，紧跟着的是常量池的数量，以及若干个常量池表项。

  `0x37`表示该 Class 文件中合计有常量池表项`55 - 1 = 54`项（常量池0为空缺项，不存放实际内容，`0x37`换算了10进制为55）。在数量之后，就是常量池的实际内容，每一项以类型、长度、内容或者类型、内容的格式存放依次排列。

  常量池可能出现的内容，其中 TAG 表示该常量的整数枚举值。
  常量池类型|TAG|常量池类型|TAG
  :--|:--:|:--|:--:
  CONSTANT_Utf8 |1 |CONSTANT_Fieldref |9
  CONSTANT_Integer |3 |CONSTANT_Methodref |10
  CONSTANT_Float |4 |CONSTANT_InterfaceMethodref |11
  CONSTANT_Long |5 |CONSTANT_NameAndType |12
  CONSTANT_Double |6 |CONSTANT_MethodHandle |15
  CONSTANT_Class |7 |CONSTANT_MethodType |16
  CONSTANT_String |8 |CONSTANT_InvokeDynamic |18

  作为常量池底层的数据类型 CONSTANT_Utf8、CONSTANT_Integer、CONSTANT_Float、CONSTANT_Long、CONSTANT_Double 分别表示 UTF8 字符串、整数、浮点数、长整型和双精度浮点常量。
  CONSTANT_Utf8 的格式如下定义，UTF8的 tag 值为1，字符串长度是 length，最后是字符串的内容：
  ```java
    CONSTANT_Utf8_info {
        u1 tag;
        u2 length;
        u1 bytes[length];
    }
  ```
  ```
  01 00 04 54 59 50 45
  ```
  `0x01`表示为一个UTF8的常量，接着`0x0004`表示该常量一共4个字节。因此，从`0x0004`之后数4个字节就为该常量的实际变量名`TYPE`。

  UTF8的常量经常被其他类型的常量引用，比如在本例中，CONSTANT_Class 常量就会引用该UTF8作为类名。其结构如下，tag 为7，表示 CONSTANT_Class 常量，第2个字段是一个两字节的整数，表示常量池索引，在 CONSTANT_Class 中，该索引指向的常量必须是 CONSTANT_Utf8：
  ```java
    CONSTANT_Class_info {
        u1 tag;
        u2 name_index;
    }
  ```