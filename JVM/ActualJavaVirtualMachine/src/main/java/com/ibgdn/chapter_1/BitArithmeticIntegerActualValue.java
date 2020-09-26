package com.ibgdn.chapter_1;

import org.junit.jupiter.api.Test;

import static java.lang.System.out;

/**
 * 通过位运算查看整数中每一位的实际值
 */
public class BitArithmeticIntegerActualValue {
    /**
     * 运行输出
     * 11111111111111111111111111110110
     * 11111111 11111111 11111111 11110110
     * <p>
     * 计算结果和之前补码的的计算结果完全匹配
     * <p>
     * 程序的基本思路是：进行32次循环（因为 int 有32位），每次循环取出 int 值中的一位，
     * 0x80000000是一个首位为1、其余位为0的整数，通过右移i位，定位到要获取的第i位，
     * 并将除该位外的其他位统一设置为0，而该位不变，最后将该位移至最右，并进行输出。
     */
    @Test
    public void BitArithmeticIntegerActualValue() {
        int number = -10;
        for (int i = 0; i < 32; i++) {
            int i1 = (number & 0x80000000 >>> i) >>> (31 - i);
            out.print(i1);
        }
    }
}
