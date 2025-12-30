package me.mdbell.awtea.util;

import lombok.Getter;

@Getter
public enum Instruction {
    // Stack load
    LOAD_CONST(0x01, 2, 1),
    LOAD_UNIFORM(0x02, 2, 1),
    LOAD_INPUT(0x03, 2, 1),
    POP(0x04, -1),
    DUP(0x05, 1),
    SWAP(0x06),
    // Arithmetic
    ADD(0x10, -1),
    SUB(0x11, -1),
    MUL(0x12, -1),
    DIV(0x13, -1),
    MOD(0x14, -1),
    NEG(0x15),
    MIN(0x16, -1),
    MAX(0x17, -1),
    ABS(0x18),
    CLAMP(0x19, -2),
    SIGN(0x1A),
    VEC_OP(0x1B, 3, Integer.MAX_VALUE),

    // Comparison/Logic
    EQ(0x20, -1),
    NEQ(0x21, -1),
    LT(0x22, -1),
    LE(0x23, -1),
    GT(0x24, -1),
    GE(0x25, -1),
    AND(0x26, -1),
    OR(0x27, -1),
    NOT(0x28),

    // Branch/Control Flow
    JUMP(0x30, 3),
    JZ(0x31, 3, -1),
    JNZ(0x32, 3, -1),
    CALL(0x33, 3),
    RET(0x34),
    CALL_NATIVE(0x35, 2, Integer.MAX_VALUE),

    // Conditionals
    SELECT(0x40, -1),

    // Textures/Drawing
    SAMPLE_SURFACE(0x50, 2, 2),
    SET_COLOR(0x51, -4),

    // Program
    END(0xFF);

    /**
     * The opcode id
     */
    private final int opcode;
    /**
     * The size of this instruction in bytes (including the opcode itself)
     */
    private final int byteSize;

    /**
     * The net change (how many elemetns are added to or removed from) for the stack
     * Integer.MAX_VALUE means that the size is variable
     */
    private final int stackChange;

    private Instruction(int opcode) {
        this(opcode, 1);
    }

    private Instruction(int opcode, int byteSize) {
        this(opcode, byteSize, 0);
    }

    private Instruction(int opcode, int byteSize, int stackChange) {
        this.opcode = opcode;
        this.byteSize = byteSize;
        this.stackChange = stackChange;
    }
}
