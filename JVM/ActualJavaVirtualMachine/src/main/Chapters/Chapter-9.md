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
        u1  tag;
        u2  length;
        u1  bytes[length];
    }
  ```
  ```
    01 00 04 54 59 50 45
  ```
  `0x01`表示为一个UTF8的常量，接着`0x0004`表示该常量一共4个字节。因此，从`0x0004`之后数4个字节就为该常量的实际变量名`TYPE`。

  UTF8的常量经常被其他类型的常量引用，比如在本例中，CONSTANT_Class 常量就会引用该UTF8作为类名。其结构如下，tag 为7，表示 CONSTANT_Class 常量，第2个字段是一个两字节的整数，表示常量池索引，在 CONSTANT_Class 中，该索引指向的常量必须是 CONSTANT_Utf8：
  ```java
    CONSTANT_Class_info {
        u1  tag;
        u2  name_index;
    }
  ```

  ```
    07 00 02 54 59 50 45
  ```
  `0x07`表示该常量为 CONSTANT_Class，`0x02`表示该类的类名由常量池第2个常量字符串指定。

  CONSTANT_Integer、CONSTANT_Float、CONSTANT_Long、CONSTANT_Double 分别表示数字的字面量。当使用 final 定义一个数字常量时，Class 文件中就会生成一个数字的常量。它们的结构分别为：
  ```
    CONSTANT_Integer_info{
        u1  tag;
        u4  bytes;
    }
    CONSTANT_Float_info{
        u1  tag;
        u4  bytes;
    }
    CONSTANT_Long_info{
        u1  tag;
        u4  high_bytes;
        u4  low_bytes;
    }
    CONSTANT_Double_info{
        u1  tag;
        u4  high_bytes;
        u4  low_bytes;
    }
  ```
  其中，tag 的值保持不变。对于 CONSTANT_Integer、CONSTANT_Float，它们的值由一个4字节的无符号整数表示，对于 CONSTANT_Long、CONSTANT_Double，它们的值由两个4字节无符号整数表示。这里以 CONSTANT_Integer 为例说明这些常量的表示方式。

  ```
    03 00 00 00 01
  ```
  `0x03`表示一个整数常量，紧接着的`00 00 00 01`表示数字1 。

  另外一个值得注意的字面量是 CONSTANT_String，它表示一个字符串常量，结构如下：
  ```
    CONSTANT_String_info {
        u1  tag;
        u2  string_index;
    }
  ```
  其中 tag 为8， 一个2字节长的无符号整数指向常量池的索引，表示该字符串对应的UTF8内容。

  另一个被广泛使用的类型为 CONSTANT_NameAndType，从名字上可以看出，它表示一个名词和类型，格式如下：
  ```
    CONSTANT_NameAndType_info {
        u1  tag;
        u2  name_index;
        u2  descriptor_index;
    }
  ```
  其中，tag 为12，第一个2字节 name_index 表示名称，意为常量池的索引，表示常量池第 name_index 项为名字，通常可以表示字段名字或者方法名字。第二个2字节 descriptor_index 表示类型的描述，比如表示方法的签名或者字段的类型。

  ```
    0C 00 09 00 06
  ```
  `0x0C`表示一个 CONSTANT_NameAndType，`0x0009`表示第9项常量为名称，査常量池，可得第9项常量为 id 字符串。`0x0006`表示第6项常量，査常量表得到字符串 I，表示 int 类型。因此，该 CONSTANT_NameAndType 表示一个名称为 id，类型为 int 的表项。

  CONSTANT_NameAndType 的 descriptor_index 使用了一组特定的字符串来表示类型，如表所示。
  字符串 |类型 |字符串  |类型
  :--|:--:|:--|:--:
  B |byte |C  |char
  D |double |F  |float
  I |int  |J  |long
  S |short  |Z  |boolean
  V |void |L; |表示对象
  [ |数组 ||
  对于对象类型来说，总是以 L 开头，紧跟类的全限定名，用分号（；）结尾，比如以字符串"Ljava/lang/Object;"表示类`java.lang.Object`。数组则以左中括号“[”作为标记，比如 String 二维数组，使用“[[Ljava/lang/String;”字符串表示。

  对于类的方法和字段，则分别使用 CONSTANT_Methodref 和 CONSTANT_Fieldref 表示。它们分别可以表示一个类的方法以及字段的引用。CONSTANT_Methodref 和 CONSTANT_Fieldref 的结构是非常类似的，如下所示：
  ```
    CONSTANT_Methodref_info {
        u1  tag;
        u2  class_index;
        u2  name_and_type_index;
    }
    CONSTANT_Fieldref_info {
        u1  tag;
        u2  class_index;
        u2  name_and_type_index;
    }
  ```
  其中 CONSTANT_Methodref 的 tag 值为10，CONSTANT_Fieldref 的 tag 值为9。它们的 class_index 表示方法或者字段所在的类在常量池中的索引，它会指向一个 CONSTANT_Class 结构。第2项 name_and_type_index 也是指向常量池的索引，但表示一个 CONSTANT_NameAndType 结构，它定义了方法或者字段的名称、类型或者签名。

  使用的`System.out.println()`方法所表示的 CONSTANT_Methodref 在常量池中的引用关系。可以看到，Methodref 结构的 class_index 字段指向了第41号常量池项，表示 Class，而该项又进一步指向常量池中的UTF8数据，表明该 Class 的类型。而 Methodref 的 name_and_ref 字段则指向常量池第43项，NameAndType 类型的数据，它包括名字和类型两个字段，又分别指向常量池中的两个字符串 println 和 (Ljava/lang/String;)V，表示方法的名字和方法的签名。(Ljava/lang/String;)V 表示该方法接收一个 String 类型的参数，并且返回值为 void。就这样，通过常量池中的引用关系.通过 Methodref 结构，将方法描述清楚了。
  ```mermaid
    graph LR
    a[Methodref class_index name_and_type_index] --> b[Class#41 name_index] --> c[UTF8 java/io/PrintStream]
    a --> d[NameAndType#43 name_index descriptor_index] --> e[UTF8 println]
    d --> f[UTF8 Ljava/lang/String V]
  ```

  对于 CONSTANT_InterfaceMethodref，它用于表示一个接口的方法。如果在 Java 程序中，出现了对接口方法的调用，那么就会在常量池中生成一个接口方法的引用。该项目的结构如下：
  ```
    CONSTANT_MethodType_info {
      u1  tag;
      u2  descriptor_index;
    }
  ```
  其中 tag 为16，descriptor_index 为指向常量池的一个UTF8字符串的索引，使用手法和前面介绍的索引如出一辙。该常量项用于描述一个方法签名，比如“()V”，表示一个不接收参数，返回值为 void 的方法。当需要传送给引导方法一个 MethodType 类型时，类文件中就会出现此项。（有关引导方法和该类型具体使用案例，可参见第11章）

  CONSTANT_MethodHandle 为一个方法句柄，它可以用来表示函数方法、类的字段或者构造函数等。方法句柄指向一个方法、字段，和 C 语言中的函数指针或者 C# 中的委托有些类似。
  它的结构如下：
  ```
    CONSTANT_MethodHandle_info {
      u1  tag;
      u1  reference_kind;
      u2  reference_index;
    }
  ```
  其中，tag 值为15，reference_kind 表示这个方法句柄的类型，reference_index 为指向常量池的索引，reference_index 具体指向的类型，由 reference_kind 确定。两者对应关系参见表。
  REF_get|
  <table>
  	<tr>
		<th>reference_kind 取值</th>
		<th>reference_index 对应类型</th>
	</tr>
	<tr>
		<td>REF_getField(1)</td>
		<td rowspan="5">常量池的指向内容必须是 CONSTANT_Fieldref 类型</td>
	<tr>
	<tr>
		<td>REF_getStatic(2)</td>
	</tr>
	<tr>
		<td>REF_putField(3)</td>
	</tr>
	<tr>
		<td>REF_putStatic(4)</td>
	</tr>
	<tr>
		<td>REF_invokeVirtual(5)</td>
        <td rowspan="4">常量池指针必须是 CONSTANT_Methodref 类型，对于 REF_invokeInterface 来说为 InterfaceMethodref 类型，且不能为 init 或者 clinit 方法（即不能为类的构造函数或者初始化方法）</td>
	</tr>
	<tr>
		<td>REF_invokeStatic(6)</td>
	</tr>
	<tr>
		<td>REF_invokeSpecial(7)</td>
	</tr>
	<tr>
		<td>REF_invokeInterface(9)</td>
	</tr>
	<tr>
		<td>REF_newInvokeSpecial(8)</td>
        <td>常量池指针必须是 CONSTANT_Methodref 类型，且对应的方法必须为 init </td>
	</tr>
  </table>

  CONSTANT_InvokeDynamic 结构用于描述一个动态调用，动态调用是 Java 虚拟机平台引入的，专门为动态语言提供函数动态调用绑定支持的功能。相关结构信息，如下所示：
  ```
    CONSTANT_InvokeDynamic_info {
      u1  tag;
      u2  bootstrap_method_attr_index;
      u2  name_and_type_index;
    }
  ```
  其中，tag 为18，bootstrap_method_attr_index 为指向引导方法表中的索引，即定位到一个引导方法。引导方法用于在动态调用时进行运行时函数查找和绑定。引导方法表属于类文件的属性（Attribute），name_and_type_index 为指向常量池的索引，且指向的表项必须是 CONSTANT_NameAndType，用于表示方法的名字以及签名。

#### 9.2.4 Class 的访问标记（Access Flag）
  在常量池后，紧跟着访问标记。该标记使用两个字节表示，用于表明该类的访问信息，如 public、final、abstract 等。

  每一种类型的表示都是通过设置访问标记的32位中的特定位来实现的。比如，若是 public final 的类，则该标记为 ACC_PUBLIC | ACC_FINAL。
  
  类 Access Flag 标记位和含义
  标记名称  |数值 |描述
  :--|:--:|:--
  ACC_PUBLIC  |0x0001 |表示 public 类（public 类可以在包外访问）
  ACC_FINAL |0x0010 |是否为 final 类（final 类不可被继承）
  ACC_SUPER |0x0020 |使用增强的方法调用父类方法
  ACC_INTERFACE |0x0200 |是否为接口
  ACC_ABSTRACT  |0x0400 |是否是抽象类
  ACC_SYNTHETIC |0x1000 |由编译器产生的类，没有源码对应
  ACC_ANNOTATION  |0x2000 |是否是注释
  ACC_ENUM  |0x4000 |是否是枚举

  标记位为`0x0021`，因此，可以判断该类为 public，且 ACC_SUPER 标记被置为1。

#### 9.2.5 当前类、父类和接口
  在访问标记后，会指定该类的类别、父类类别以及实现的接口，格式如下：
  ```
    u2  this_class;
    u2  super_class;
    u2  interfaces_count;
    u2  interfaces[interfaces_count];
  ```
  其中，this_class、super_class 都是2字节无符号整数，它们指向常量池中一个 CONSTANT_Class，以表示当前的类型以及父类。由于在 Java 中只能使用单继承，因此，只需要保存单个父类即可。

  注意：**super_class 指向的父类不能是 final**。 

  由于一个类可以实现多个接口，因此需要以数组形式保存多个接口的索引，表示接口的每个索引也是一个指向常量池的 CONSTANT_Class（当然这里就必须是接口，而不是类）。如果该类没有实现任何接口，则 interface_count 为0。 

#### 9.2.6 Class 文件的字段
  在接口描述后，就会有类的字段信息。由于一个类会有多个字段.因此，需要首先指明字段的个数：
  ```
    u2          fields_count;
    field_info  fields[fields_count];
  ```

  字段的数量 fields_count 是一个2字节无符号整数。字段数量之后为字段的具体信息，每一个字段为一个 field_info 的结构，该结构如下：
  ```
    field_info {
      u2              access_flags;
      u2              name_index;
      u2              descriptor_index;
      u2              attributes_count;
      attribute_info  attributes[attributes_count];
    }
  ```