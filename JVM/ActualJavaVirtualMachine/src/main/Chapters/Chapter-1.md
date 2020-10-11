### 1.5 数字编码就是计算机世界的水和电
#### 1.5.1 整数在 Java 虚拟机中的表示
1. **原码**
    原码，就是符号位加上数字的二进制表示。

    以 int 数据类型为例，第1位表示符号位（整数或者负数），其余31位表示该数字的二进制数值。

    10 的原码为：`00000000 00000000 00000000 00001010`

    -10的原码为：`10000000 00000000 00000000 00001010`

    对于原码来说，绝对值相同的正数和负数只有符号位不同。

2. **反码**
    反码在原码的基础上，符号位保持不变，其余位置取反。

    以10为例，反码为：`01111111 11111111 11111111 11110101`

3. **补码**
    负数的补码就是反码`+1`，整数的补码就是原码本身。

    10 的补码：`00000000 00000000 00000000 00001010`

    -10的补码：`11111111 11111111 11111111 11110110`

    [通过位运算查看整数中每一位的实际值](../java/com/ibgdn/chapter_1/BitArithmeticIntegerActualValue.java)。

4. **使用补码作为计算机内的实际存储方式的好处**

- 统一数字0的表示。
    0既非正数，也非负数，使用原码表示时符号位难以确定，把0归入正数或负数得到的原码编码不同。如果使用补码表示，无论0归入正数或者负数，都会得到相同的结果。

    - 0为正数
        0的补码为原码本身：`00000000 00000000 00000000 00000000`

    - 0为负数
        0的原码：`10000000 00000000 00000000 00000000`

        反码：`11111111 11111111 11111111 11111111`

        补码：`00000000 00000000 00000000 00000000`

    使用补码作为整数编码，可以解决数字0的存储问题。

- 简化整数加减法运算
    将减法运算视为加法运算，实现加法和减法的统一，实现正数和负数加法的统一。

    例如：计算`-6 + 5`，过程如下：

    -6补码：`1111 1010`，5的补码：`0000 0101`，直接相加得：`1111 1111`，`-1`。

    计算`4 + 6`，过程如下：

    4的补码：`0000 0100`，6的补码：`0000 0110`，直接相加得：`0000 1010`，`10`。

    使用补码表示整数时，只需将补码简单相加，可得到算术加法的正确结果，无须对正数或负数做区别对待。

#### 1.5.2 浮点数在 Java 虚拟机中的表示

  最为广泛使用的是由 IEEE754 定义的浮点数格式。

  一个浮点数一般由3部分组成：符号位、指数位和尾数位。32位 float 数值，符号位占1位，表示正负数，指数位占8位，尾数位占剩余的23位。

  获取一个单精度浮点数的 IEEE754 表示：
  ```java
    float floatNum = -5;
    System.out.println(Integer.toBinaryString(Float.floatToRawIntBits(a)));
  ```