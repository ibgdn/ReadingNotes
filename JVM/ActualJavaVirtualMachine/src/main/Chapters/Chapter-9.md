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

  **Class 文件版本号和平台的对应**
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
  **类型的字符串表示方法**
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

  **类 Access Flag 标记位和含义**
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

  1. 首先是字段的访问标记，非常类似于类的访问标记，该字段的取值参见表
  **字段 Access Flag 标记位和含义**
  标记名称  |数值 |描述
  :--|:--:|:--
  ACC_PUBLIC  |0x0001 |表示 public 字段
  ACC_PRIVATE |0x0002 |表示 private 私有字段
  ACC_PROTECTED |0x0004 |表示 protected 保护字段
  ACC_STATIC  |0x0008 |表示静态字段
  ACC_FINAL |0x0010 |是否为 final 字段（final 字段表示常量）
  ACC_VOLATILE  |0x0040 |是否为 volatile
  ACC_TRANSIENT |0x0080 |是否为瞬时字段，表示在持久化读写时，忽略该字段
  ACC_SYNTHETIC |0x1000 |由编译器产生的方法，没有源码对应
  ACC_ENUM  |0x4000 |是否是枚举

  2. 紧接着，是一个2字节整数，表示字段的名称，它指向常量池中的 CONSTANT_Utf8 结构。

  3. 名称后的 descriptor_index 也指向常量池中 CONSTANT_Utf8，该字符用于描述字段的类型。

  4. 一个字段还可能拥有一些属性，用于存储更多的额外信息，比如初始化值、一些注释信息等。属性个数存放在 attributes_count 中，属性具体内容存放于 attributes 数组。
     
     以常量属性为例，常量属性的结构为：
  ```
    ConstantValue_attribute {
      u2  attribute_name_index;
      u4  attribute_length;
      u2  constantvalue_index;
    }
  ```
  常量属性的 attribute_name_index 为2字节整数，指向常量池的 CONSTANT_Utf8，并且这个字符串为“ConstantValue”。接着，为 attribute_length，它由4个字节组成，表示这个属性的剩余长度为多少。对常量属性而言，这个值恒为2。最后的 constantvalue_index 表示属性值，但值并不直接出现在属性中，而是存放在常量池中，这里的 constantvalue_index 也是指向常量池的索引。这表示，一个 int 类型字段的常量，constantvalue_index 指向的常量池类型必须是 CONSTANT_Integer。

  **常量数据类型和常量池类型对应关系**
  字段类型  |常量池表项类型
  :--|:--
  long  |CONSTANT_Long
  float |CONSTANT_Float
  double  |CONSTANT_Double
  int,short,char,byte,boolean |CONSTANT_Integer
  String  |CONSTANT_String

  ```
  00 03 00 19 00 05 00 06 00 01 00 07 00 00 00 02 00 08
  ```
  首先`0x0003`表示该类存在3个字段。`0x0019`为第一个字段的访问标记，`0x0019`=ACC_PUBLIC|ACC_STATIC|ACC_FINAL，因此这个字段是一个`public static final`的字段。接着，`0x0005`为常量池索引，表示字段名称，査常量池表可得字符串“TYPE”，`0x0006`为字段的类型描述，査常量池表得字符串“I”。由此，看到这是一个类型为 int，变量名为 TYPE 的`public static final`常量。接着为属性数量，值为`0x0001`，表示该字段存在1个属性，`0X0007`为属性名，通过该值确认属性的类型。査常量池第7项，为字符串“ConstantValue”，表示该属性为常量属性。之后，连续4个字节`0x00000002`为属性的剩余长度，这里表示从`0x00000002`之后的两个字节为属性的全部内容。本例中，该值为0x0008，它表示属性值需要査阅常量池第8项。査找常量池第8项，得常量 CONSTANT_Integer，值为1。所以该字段的常量值为1，类型为 int。

#### 9.2.7 Class 文件的方法基本结构
  在字段之后，就是类的方法信息。方法信息和字段类似，由两部分组成：
  ```
    u2          methods_count;
    method_info methods[methods_count];
  ```

  其中 methods_count 为2字节整数，表示该类中有几个方法。接着就是 methods_count 个 method_info 结构，每一个 method_info 表示一个方法，该结构如下：
  ```
    method_info {
      u2              access_flags;
      u2              name_index;
      u2              descriptor_index;
      u2              attributes_count;
      attribute_info  attributes[attributes_count];
    }
  ```

  1. access_flag 为方法的访问标记，用于标明方法的权限以及相关特性。
     **方法访问标记取值**
     标记名称 |值  |作用
     :--|:--|:--
     ACC_PUBLIC |0x0001 |public 方法
     ACC_PRIVATE    |0x0002 |private 私有方法
     ACC_PROTECTED  |0x0004 |protected 方法
     ACC_STATIC |0x0008 |静态方法
     ACC_FINAL  |0x0010 |final 方法，不可被继承重载
     ACC_SYNCHRONIZED   |0x0020 |synchronized 同步方法
     ACC_BRIDGE |0x0040 |由编译器产生的桥梁方法
     ACC_VARARGS    |0x0080 |可变参数的方法
     ACC_NATIVE |0x0100 |native 本地方法
     ACC_ABSTRACT   |0x0400 |抽象方法
     ACC_STRICT |0x0800 |浮点模式为 FP-strict
     ACC_SYNTHETIC  |0x1000 |编译器产生的方法，没有源码对应

  2. 在访问标记后，name_index 表示方法的名称，它是一个指向常量池的索引。descriptor_index 为方法描述符，它也是指向常量池的索引，是一个字符串，用以表示方法的签名（参数、返回值等），它基于**类型的字符串表示方法**表所示的字符串的类型表示方法，同时对方法签名的表示做了一些规定。它将函数的参数类型写在一对小括号中，并在括号右侧给出方法的返回值。比如，若有如下方法：
     ```java
        Object m (int i, double d, Thread t) {...}
     ```
     则它的方法描述符为：
     ```
        (IDLjava/lang/Thread;)Ljava/lang/Object;
     ```
     可以看到，方法的参数统一列在一对小括号中，“I”表示 int，“D”表示 double，“Ljava/lang/Thread”表示 Thread 对象。小括号右侧的 Ljava/lang/Object; 表示方法的返回值为 Object 对象。

  3. 和字段类似，方法也可以附带若干个属性，用于描述一些额外信息，比如方法字节码等，attributes_count 表示该方法中属性的数量，紧接着，就是 attributes_count 个属性的描述。
     对于属性 attribute 来说，它们的统一格式为：
     ```
        attribute_info {
            u2  attribute_name_index;
            u4  attribute_length;
            u1  info[attribute_length];
        }
     ```
     其中，attribute_name_index 表示当前 attribute 的名称，attribute_length 为当前 attribute 的剩余长度，紧接着就是 attribute_length 个字节的 byte 数组。
     
     **常用属性 Attribute**
     属性 Attribute|作用
     :--|:--
     ConstantValue  |用于字段常量
     Code   |表示方法的字节码
     StackMapTable  |Code 属性的描述属性，用于字节码变量类型验证
     Exceptions |方法的异常信息
     SourceFile |类文件的属性，表示生成这个文件的源码
     LineNumberTable    |Code 属性的描述属性，描述行号和字节码的对应关系
     LocalVariableTable |Code 属性的描述属性，描述函数局部变量表
     BootstrapMethods   |类文件的描述属性，存放类的引导方法。用于 invokeDynamic
     StackMapTable  |Code 属性的描述属性，用于字节码类型校验

#### 9.2.8 方法的执行主体——Code 属性
  方法的主要内容存放在其属性之中，而当中最为重要的一个属性就是 Code。它存放着方法的字节码等信息，结构如下：
  ```
    Code_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  max_stack;
        u2  max_locals;
        u4  code_length;
        u1  code[code_length];
        u2  exception_table_length;
        {
            u2  start_pc;
            u2  end_pc;
            u2  handler_pc;
            u2  catch_type;
        } exception_table[exception_table_length];
        u2  attributes_count;
        attribute_info  attributes[attributes_count];
    }
  ```
  Code 属性的第一个字段 attribute_name_index 指定了该属性的名称，它是一个指向常量池的索引，指向的类型为 CONSTANT_Utf8，对于 Code 属性来说，该值恒为“Code”。接着，attribute_length 指定了 Code 属性的长度，该长度不包括前6个字节，也就是剩余长度。

  在方法执行过程中，操作数栈可能不停地变化，在整个执行过程中，操作数栈存在一个最大深度，该深度由 max_stack 表示。同理，在方法执行过程中，局部变量表也可能会不断变化。在整个执行过程中局部变量表的最大值由 max_locals 表示，它们都是2字节的无符号整数。

  在 max_locals 之后，就是作为方法的最重要部分——字节码。它由 code_length 和 code[code_length] 两部分组成，code_length 表示字节码的长度，为4字节无符号整数，code[code_length] 为 byte 数组，为字节码内容本身。

  在字节码之后，存放该方法的异常处理表。异常处理表告诉一个方法该如何处理字节码中可能抛出的异常。异常处理表亦由两部分组成：表项数量和内容。其中 exception_table_length 表示异常表的表项数量，exception_table[exception_table_length] 结构为异常表。表中每一行由4部分组成，分别是 start_pc、end_pc、handler_pc 和 catch_type。这4项表示从方法字节码的 start_pc 偏移量开始到 end_pc 偏移量为止的这段代码中，如果遇到了 catch_type所指定的异常，那么代码就跳转到 handler_pc 的位置执行。在这4项中，start_pc、end_pc 和 handler_pc 都是字节码的编译量，也就是在 code[code_length] 中的位置，而 catch_type 为指向常量池的索引，它指向一个 CONSTANT_Class 类，表示需要处理的异常类型。

  至此，Code 属性的主体部分己经介绍完毕，但是 Code 属性中还可能包含更多信息，比如行号、局部变量表等。这些信息都以 attribute 属性的形式内嵌在 Code 属性中，即除了字段、方法和类文件可以内嵌属性外，属性本身也可以内嵌其他属性。

#### 9.2.9 记录行号——LineNumberTable 属性
  Code 属性本身也包含着其他属性以进一步存储一些额外信息。首先，来看一下 LineNumberTable，它是 Code 属性的属性，用于描述 Code 属性。LineNumberTable 用来记录字节码偏移量和行号的对应关系，在软件调试时，该属性有着至关重要的作用，若没有它，则调试器无法定位到对应的源码。LineNumberTable 属性的结构如下： 
  ```
    LineNumberTable_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  line_number_table_length;
        {
            u2  start_pc;
            u2  line_number;
        }   line_number_table[line_number_table_length];
    }
  ```
  其中，attribute_name_index 为指向常量池的索引，在 LineNumberTable 属性中，该值为“LineNumberTable”，attribute_length 为4字节无符号整数，表示属性的长度（不含前6个字节），line_number_table_length 表明了表项有多少条记录，line_number_table 为表的实际内容，它包含 line_number_table 个<start, line_number>元组，其中，为字节码偏移量，line_number 为对应的行号。

#### 9.2.10 保存局部变量和参数——LocalVariableTable 属性
  对 Code 属性而言，另外一个重要的属性是 LocalVariableTable，也就是局部变量表。它记录了一个方法中所有的局部变量，它的结构如下：
  ```
    LocalVariableTable_attribute {
        u2  attributer_name_index;
        u4  attributer_length;
        u2  local_variable_table_length;
        {
            u2  start_pc;
            u2  length;
            u2  name_index;
            u2  descriptor_index;
            u2  index;
        }   local_variable_table[local_variable_table_length];
    }
  ```
  其中，attribute_name_index 为当前属性的名字，它是指向常量池的索引。对局部变量表而言，该值为“LocalVariableTable”，attribute_length 为属性的长度，local_variable_table_length 为局部变量表表项条目。

  局部变量表的每一条记录由以下几个部分组成。
  - start_pc、length：表示当前局部变量的开始位置（start_pc）和结束位置（start_pc + length 不含最后一个字节）。
  - name_index：局部变量的名称，这是一个指向常量池的索引。
  - descriptor_index：局部变量的类型描述，指向常量池的索引。使用和字段描述符一样的方式描述局部变量。
  - index：局部变量在当前帧栈的局部变量表中的槽位。对于 long 和 double 的数据，它们会占据局部变量表中的两个槽位。

#### 9.2.11 加快字节码校验——StackMapTable 属性
  对于JDK1.6以后的类文件，每个方法的 Code 属性还可能含有一个 StackMapTable 的属性结构。该结构中存有若干个叫做栈映射帧 （stack_map_frame）的数据。该属性不包含运行时所需的信息，仅用作 Class 文件的类型校验。

  StackMapTable 的结构如下：
  ```
    StackMapTable_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  number_of_entries;
        stack_map_frame entries[number_of_entries];
    }
  ```
  其中，attribute_name_index 为常量池索引，恒为“StackMapTable”，attribute_length 为该属性的长度，number_of_entries 为栈映射帧的数量，最后的 entries 则为具体的内容，每一项为一个 stack_map_frame 结构。

  每一个栈映射帧都是为了说明在一个特定的字节码偏移位置上，系统的数据类型是什么（包括局部变量表的类型和操作数栈的类型）。每一帧都会显式或者隐式地指定一个字节码偏移量的变化值 offset_delta，使用 offset_delta 可以计算出这一帧数据的字节码偏移位置。计算方法就是将`offset_delta + 1`和上一帧的字节码偏移量相加。如果上一帧是方法的初始帧，那么，字节码偏移量为 offset_delta 化本身。

  注意：这里说的“帧”，和帧栈的帧不是同一个概念。这里更接近于一个跳转语句，跳转语句将函数划分成不同的块，每一块的概念就接近于这里所说的栈映射帧中的“帧”。

  StackMapTable 结构中的 stack_map_frame 被定义为一个枚举值，它可能的取值如下：
  ```
    union stack_map_frame {
        same_frame;
        same_locals_l_stack_item_frame;
        same_locals_l_stack_item_frame_extended;
        chop_frame;
        same_frame_extended;
        append_frame;
        full_frame;
    }
  ```

  1. 第1个取值 same_frame 定义如下：
  ```
    same_frame {
        u1  frame_type = SAME; /* 0-63 */
    }
  ```
  它表示当前代码所在位置和上一个比较位置的局部变量表是完全相同的，并且操作数栈为空。它的取值为0-63，这个取值也是隐含的 offset_delta，表示距离上一个帧块的偏移量。

  2. 第2个取值 same_local_l_stack_item_frame 的定义如下：
  ```
    same_local_l_stack_item_frame {
        u1  frame_type = SAME_LOCALS_l_STACK_ITEM; /* 64-127 */
        verification_type_info  stack[1];
    }
  ```
  其中，frame_type 的范围为64-127，如果栈映射帧为该值，则表示当前帧和上一帧有相同的局部变量，并且操作数栈中变量数量为1。它有一个隐式的 offset_delta，使用`frame_type - 64`可以计算得来。之后的 verification_type_info 就表示该操作数中的变量类型。

  3. 第3个取值 same_locals_l_stack_item_frame_extended，和 same_locals_l_stack_item_frame 含义相同，但是前者表示的 offset_delta 非常有限，如果超出范围，则需要使用 same_locals_l_stack_item_frame_extended，same_locals_l_stack_item_frame_extended 使用显式的 offset_delta。同样，在结构的最后，存放着操作数栈的数据类型，结构如下：
  ```
    same_locals_l_stack_item_frame_extended {
        u1  frame_type = SAME_LOCALS_l_STACK_ITEM_EXTENDED; /* 247 */
        u2  offset_delta;
        varification_type_info  stack[1];
    }
  ```

  4. 第4个取值 chop_frame 则表示操作数桟为空，当前局部变量表比前一帧少 k 个局部变量。其中，k 为`251 - frame_type`。它的结构如下：
  ```
    chop_frame {
        u1  frame_type = CHOP;  /* 248-250 */
        u2  offset_delta;
    }
  ```

  5. 第5个取值 same_frame_extended 和 same_frame 含义一样，表示局部变量信息和上一帧相同，且操作数桟为空。但是 same_frame_extended 显示指定了 offset_delta，可以表示更大的字节偏移量。它的结构如下：
  ```
    same_frame_extended {
        u1  frame_type = SAME_FRAME_EXTENDED;   /* 251 */
        u2  offset_delta;
    }
  ```

  6. 第6个取值 append_frame 表示当前帧比上一帧多了 k 个局部变量，且操作数栈为空。其中 k 为`frame_type - 251`。在 append_frame 的最后，还存放着增加的局部变量的类型。它的结构如下：
  ```
    append_frame {
        u1  frame_type = APPEND;    /* 252 - 254 */
        u2  offset_delta;
        verification_type_info  locals[frame_type - 251];
    }
  ```

  7. 以上类型均只保存了前后两个帧中变化的部分，因此可以减少数据的大小。但是，如果以上结构都无法表达帧的信息时，则可以使用第7种结构 full_frame。它不用来表示连续两个帧之间的差异，而是将局部变量表和操作数栈都做了完整的记录。它的结构如下：
  ```
    full_frame {
        u1  frame_type = FULL_FRAME;    /* 255 */
        u2  offset_delta;
        u2  number_of_locals;
        verification_type_info  locals[number_of_locals];
        u2  number_of_stack_items;
        verification_type_info  stack[number_off_stack_items];
    }
  ```
  可以看到，在 full_frame 中，显示指定了 offset_delta，完整记录了局部变量表的数量（number_of_locals）、局部变量表的数据类型（locals）、操作数栈的数量（number_of_stack_items）和操作数栈的类型（stack）。

#### 9.2.12 Code 属性总结
  ```mermaid
  graph LR
  a[Method结构 访问符 名字 描述] --> b[Code属性 最大操作数栈 最大局部变量表 方法字节码 异常处理表]
  b --> c[LineNumberTable start_pc->line number ...]
  b --> d[LocalVaribaleTable 局部变量范围 变量名 变量类型 槽位]
  b --> e[StackMapTable 帧差异描述]
  ```

#### 9.2.13 抛出异常——Exception 属性
  除了 Code 属性外，每一个方法都可以有一个 Exceptions 属性，用于保存该方法可能抛出的异常信息。该属性的结构如下：
  ```
    Exceptions_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  number_of_exceptions;
        u2  exception_index_table[number_of_exceptions];
    }
  ```
  Exceptions 与 Code 属性中的异常表不同。Exceptions 属性表示一个方法可能抛出的异常，通常是由方法的 throws 关键字指定的。而 Code 属性中的异常表，则是异常处理机制，由 try-catch 语句生成。

  下面的代码将生成带有 Exceptions 属性的方法：
  ```java
    public static void main (String[] args) throws java.io.IOException {
    
    }
  ```
  Exceptions 属性表中，attribute_name_index 指定了属性的名称，它为指向常量池的索引，恒为“Exceptions”，attribute_length 表示属性长度，number_of_exceptions 表示表项数量即可能抛出的异常个数，最后 exception_index_table 项罗列了所有的异常，每一项为指向常量池的索引，对应的常量为 CONSTANT_Class，为一个异常类。

#### 9.2.15 我来自哪里——SourceFile 属性
  SourceFile 属性属于 Class 文件的属性。它用于描述当前这个 Class 文件是由哪个源代码文件编译得来的，格式如下：
  ```
    SourceFile_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  sourcefile_index;
    }
  ```

  其中，attribute_name_index 表示属性名称，为指向常量池的一个索引，这里恒为“SourceFile”。attribute_length 为属性长度，对于 SourceFile 属性来说，恒为2。最后的 sourcefile_index 表示源代码文件名，它是指向常量池的索引，为CONSTANT_Utf8类型。

#### 9.2.16 强大的动态调用——BootstrapMethods 属性
  为了支持JDK1.7中的 invokeDynamic 指令，Java 虚拟机增加了 BootstrapMethods 属性，它用于描述和保存引导方法。引导方法可以理解成是一个査找方法的方法，invokeDynamic 需要能够在运行时根据实际情况返回合适的方法调用，而使用何种策略去査找所需要的方法，是由引导方法决定的。这里的 BootstrapMethods 属性就是用于找到调用的目标方法。该属性是 Class 文件的属性，属性结构如下：
  ```
    BootstrapMethods_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  num_bootstrap_methods;
        {   u2  bootstrap_method_ref;
            u2  num_bootstrap_arguments;
            u2  bootstrap_arguments[num_bootstrap_arguments];
        } bootstrap_methods[num_bootstrap_methods];
    }
  ```

  第1个字段 attribute_name_index 表述属性名称，为指向常量池的索引。在这里为“BootstrapMethods”。第2个字段 attribute_length 为4字节码数字，表示属性的总长度（不含这前6个字节）。第3个字段 num_bootstrap_methods 表示这个类中包含的引导方法的个数。之后就是 num_bootstrap_methods 个 bootstrap_methods 引导方法。

  每一个引导方法又由3个字段构成，含义如下。
  - bootstrap_method_ref：必须是指向常量池的常数，并且入口为 CONSTANT_MethodHandle，用于指名函数。 
  - num_bootstrap_arguments：指明引导方法的参数个数。
  - bootstrap_arguments：引导方法的参数类型。这是一个指向常量池的索引，且常量池入口只能是：CONSTANT_String、CONSTANT_Class、CONSTANT_Integer、CONSTANT_Long、CONSTANT_Float、CONSTANT_Double、CONSTANT_MethodHandle 或者 CONSTANT_MethodType。这也表示，引导方法也只能接受以上类型的参数。

#### 9.2.17 内部类——InnerClasses 属性
  InnerClass 属性是 Class 文件的属性，它用来描述外部类和内部类之间的联系，其结构如下：
  ```
    InnerClasses_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
        u2  number_of_classes;
        {   u2  inner_class_info_index;
            u2  outer_class_info_index;
            u2  inner_name_index;
            u2  inner_class_access_flags;
        } classes[number_of_classes];
    }
  ```
  其中，attribute_name_index 表示属性名称，为指向常量池的索引，这里恒为“InnerClasses”。attribute_length 为属性长度，number_of_classes 表示内部类的个数。classes[number_of_classes] 为描述内部类的表格，每一条内部类记录包含4个字段，其中，inner_class_info_index 为指向常量池的指针，它指向一个 CONSTANT_Class，表示内部类的类型。outer_class_info 表示外部类类型，也是常量池的索引。inner_name_index 表示内部类的名称，指向常量池中的CONSTANT_Utf8项。最后的 inner_class_access_flags 为内部类的访问标识符，用于指示 static 等属性。


  **常用属性 Attribute**
  访问标记  |值  |含义
  :--|:--|:--
  ACC_PUBLIC    |0x0001 |public 公告类
  ACC_PRIVATE   |0x0002 |私有类
  ACC_PROTECTED |0x0004 |受保护的类
  ACC_STATIC    |0x0008 |静态内部类
  ACC_FINAL |0x0010 |final 类
  ACC_INTERFACE |0x0200 |接口
  ACC_ABSTRACT  |0x0400 |抽象类
  ACC_SYNTHETIC |0x1000 |编译器产生的，非代码产生的类
  ACC_ANNOTATION    |0x2000 |注释
  ACC_ENUM  |0x4000 |枚举

#### 9.2.18 将要废弃的通知——Deprecated 属性
  Deprecated 属性可以用在类、方法、字段等结构中，用于表示该类、方法或者字段将在未来版本中被废弃。Deprecated 属性的结构如下：
  ```
    Deprecated_attribute {
        u2  attribute_name_index;
        u4  attribute_length;
    }
  ```

  其中，attribute_name_index 表示属性名，为指向常量池的索引，在这里恒为“Deprecated”，attribute_length 为0。当一个类、方法或者字段被标记为 Deprecated 时，就会产生这个属性。在生成的 SimpleDe 类中，就含有 Deprecated 属性，如下代码所示：
  ```java
    @Deprecated
    public class SimpleDe {}
  ```

### 9.3 操作字节码：走进 ASM
  ASM 是一款 Java 字节码的操作库，它在 Java 领域是赫赫有名的函数库。不少著名的软件都依赖该库进行字节码操作。比如，AspectJ、Clojure、Eclipse、Spring 以及 CGLIB 都是 ASM 的使用者。与 CGLIB 和 Javasssit 等高层字节码库相比，ASM 的性能远远超过它们，由于它直接工作于底层，因此使用也更为灵活，功能也更为强大。但是由于其使用复杂，要求开发人员熟悉和掌握 Class 文件的基本格式和 Java 的字节码，对开发人员要求相对较高，因此很少被直接使用。

#### 9.3.1 ASM 体系结构
  其中 Opcodes 接口定义了一些常量，尤其是版本号、访问标示符、字节码等信息。ClassReader 用于读取 Class 文件，它的作用是进行 Class 文件的解析，并可以接受一个 ClassVisitor，ClassReader 会将解析过程中产生的类的部分信息，比如访问标示符、字段、方法逐个送入 ClassVisitor，ClassVisitor 在接收到对应的信息后，可以进行各自的处理。

  ClassVisitor 有一个重要的子类为 ClassWriter，它负责进行 Class 文件的输出和生成。ClassVisitor 在进行字段和方法处理的时候，会委托给 FieldVisitor 和 MethodVisitor 进行处理。因此，在类的处理过程中，会创建对应的 FieldVisitor 和 MethodVisitor 对象。FieldVisitor 和 MethodVisitor 类也各自有1个重要的子类，FiledWriter 和 MethodWriter。当 ClassWriter 进行字段和方法的处理时，也是依赖这两个类进行的。