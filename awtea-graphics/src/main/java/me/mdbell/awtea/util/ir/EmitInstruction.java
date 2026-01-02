package me.mdbell.awtea.util.ir;

import java.util.Arrays;
import java.util.List;

import me.mdbell.awtea.util.Instruction;

public class EmitInstruction implements EmitNode {
    public final Instruction opcode;
    public final List<Object> args;

    public EmitInstruction(Instruction opcode, Object... args) {
        this.opcode = opcode;
        this.args = Arrays.asList(args);
    }
}
