package com.example.json;

public class BitOpsDemo {
    public static void main(String[] args) {
        int a = 0b1100; // 12
        int b = 0b1010; // 10

        System.out.printf("a = %s (%d)%n", Integer.toBinaryString(a), a);
        System.out.printf("b = %s (%d)%n", Integer.toBinaryString(b), b);
        System.out.printf("a & b = %s (%d)%n", Integer.toBinaryString(a & b), (a & b));
        System.out.printf("a | b = %s (%d)%n", Integer.toBinaryString(a | b), (a | b));
        System.out.printf("a ^ b = %s (%d)%n", Integer.toBinaryString(a ^ b), (a ^ b));
        System.out.printf("~a = %s (%d)%n", Integer.toBinaryString(~a), ~a);

        int value = 0b101100; // 44
        System.out.println();
        System.out.printf("value = %s (%d)%n", Integer.toBinaryString(value), value);
        System.out.printf("test bit 2: %b%n", (value & (1 << 2)) != 0); // bit index 2 (0-based)
        value |= (1 << 1); // set bit 1
        System.out.printf("after set bit1: %s (%d)%n", Integer.toBinaryString(value), value);
        value &= ~(1 << 3); // clear bit 3
        System.out.printf("after clear bit3: %s (%d)%n", Integer.toBinaryString(value), value);
        value ^= (1 << 2); // toggle bit 2
        System.out.printf("after toggle bit2: %s (%d)%n", Integer.toBinaryString(value), value);

        System.out.println();
        int lowest = value & -value;
        System.out.printf("lowest set bit isolated: %s (%d)%n", Integer.toBinaryString(lowest), lowest);

        System.out.println();
        System.out.printf("Integer.bitCount(%d) = %d%n", value, Integer.bitCount(value));
        System.out.printf("Leading zeros: %d%n", Integer.numberOfLeadingZeros(value));
        System.out.printf("Highest set bit index: %d%n", 31 - Integer.numberOfLeadingZeros(value));
    }

    // test bit
    static boolean testBit(int v, int i) { return (v & (1 << i)) != 0; }
    // set bit
    static int setBit(int v, int i) { return v | (1 << i); }
    // clear bit
    static int clearBit(int v, int i) { return v & ~(1 << i); }
    // toggle
    static int toggleBit(int v, int i) { return v ^ (1 << i); }
    // isolate lowest set bit
    static int isolateLowest(int v) { return v & -v; }

}

