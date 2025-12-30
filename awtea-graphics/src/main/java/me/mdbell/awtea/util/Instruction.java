package me.mdbell.awtea.util;

import lombok.Getter;

@Getter
public enum Instruction {
    // Stack load
    LOAD_CONST(0x01, 2),
    LOAD_UNIFORM(0x02, 2),
    LOAD_INPUT(0x03, 2),
    POP(0x04),
    DUP(0x05),
    SWAP(0x06),
    // Arithmetic
    ADD(0x10),
    SUM(0x11),
    MUL(0x12),
    DIV(0x13),
    MOD(0x14),
    NEG(0x15),
    MIN(0x16),
    MAX(0x17),
    ABS(0x18),
    CLAMP(0x19),
    SIGN(0x1A),
    VEC_OP(0x1B, 3),

    // Comparison/Logic
    EQ(0x20),
    NEQ(0x21),
    LT(0x22),
    LE(0x23),
    GT(0x24),
    GE(0x25),
    AND(0x26),
    OR(0x27),
    NOT(0x28),

    // Branch/Control Flow
    JUMP(0x30, 3),
    JZ(0x31, 3),
    JNZ(0x32, 3),
    CALL(0x33, 3),
    RET(0x34),
    CALL_NATIVE(0x35, 2),

    // Conditionals
    SELECT(0x40),

    // Textures/Drawing
    SAMPLE_SURFACE(0x50, 2),
    SET_COLOR(0x51),

    // Program
    END(0xFF);

    private final int opcode;
    private final int byteSize;

    private Instruction(int opcode) {
        this(opcode, 1);
    }

    private Instruction(int opcode, int byteSize) {
        this.opcode = opcode;
        this.byteSize = byteSize;
    }
}
