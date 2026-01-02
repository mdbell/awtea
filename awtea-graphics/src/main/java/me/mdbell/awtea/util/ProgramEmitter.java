package me.mdbell.awtea.util;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import me.mdbell.awtea.util.ir.EmitDefine;
import me.mdbell.awtea.util.ir.EmitInstruction;
import me.mdbell.awtea.util.ir.EmitDefine.Type;
import me.mdbell.awtea.util.ir.EmitLabel;
import me.mdbell.awtea.util.ir.EmitNode;

public class ProgramEmitter {

    @Getter
    private final List<EmitNode> nodes = new ArrayList<>();

    // metadata operations

    ProgramEmitter define(Type t, String name, float value) {
        nodes.add(new EmitDefine(t, name, value));
        return this;
    }

    public ProgramEmitter defineConstant(String name, float value) {
        return define(Type.CONST, name, value);
    }

    public ProgramEmitter defineConstant(float value) {
        return define(Type.CONST, null, value);
    }

    public ProgramEmitter defineUniform(String name, float defaultValue) {
        return define(Type.UNIFORM, name, defaultValue);
    }

    public ProgramEmitter defineUniform(String name) {
        return defineUniform(name, 0);
    }

    public ProgramEmitter defineUniform(float value) {
        return defineUniform(null, value);
    }

    public ProgramEmitter defineInput(String name) {
        // input values are generated at runtime, so value is always 0
        return define(Type.INPUT, name, 0);
    }

    public ProgramEmitter defineInput() {
        return defineInput(null);
    }

    public ProgramEmitter label(String name) {
        nodes.add(new EmitLabel(name));
        return this;
    }

    ProgramEmitter load(Type t, String name) {
        Instruction insn;
        switch (t) {
            case CONST:
                insn = Instruction.LOAD_CONST;
                break;
            case UNIFORM:
                insn = Instruction.LOAD_UNIFORM;
                break;
            case INPUT:
                insn = Instruction.LOAD_INPUT;
                break;
            default:
                throw new RuntimeException("Unsupported load type:" + t);
        }
        nodes.add(new EmitInstruction(insn, name));
        return this;
    }

    ProgramEmitter load(Type t, int index) {
        Instruction insn;
        switch (t) {
            case CONST:
                insn = Instruction.LOAD_CONST;
                break;
            case UNIFORM:
                insn = Instruction.LOAD_UNIFORM;
                break;
            case INPUT:
                insn = Instruction.LOAD_INPUT;
                break;
            default:
                throw new RuntimeException("Unsupported load type:" + t);
        }
        nodes.add(new EmitInstruction(insn, index));
        return this;
    }

    public ProgramEmitter loadConst(String name) {
        return load(Type.CONST, name);
    }

    public ProgramEmitter loadConst(int index) {
        return load(Type.CONST, index);
    }

    public ProgramEmitter pop() {
        nodes.add(new EmitInstruction(Instruction.POP));
        return this;
    }

    public ProgramEmitter dup() {
        return dup(1);
    }

    public ProgramEmitter swap() {
        nodes.add(new EmitInstruction(Instruction.SWAP));
        return this;
    }

    public ProgramEmitter dup(int count) {
        if (count == 1) {
            nodes.add(new EmitInstruction(Instruction.DUP));
        } else {
            nodes.add(new EmitInstruction(Instruction.DUP_X, count));
        }
        return this;
    }

    public ProgramEmitter add() {
        nodes.add(new EmitInstruction(Instruction.ADD));
        return this;
    }

    public ProgramEmitter add(float constant) {
        // TODO: should check if a constant already exists, and only define
        // a new one if it doesn't... Or leave this as-is
        // and let us optimize that later

        // Note: This (and other methods) currently assume that defineConstant()
        // will also load the constant... But that is not the case presently. Needs to
        // be fixed
        return defineConstant(constant).add();
    }

    public ProgramEmitter sub() {
        nodes.add(new EmitInstruction(Instruction.SUB));
        return this;
    }

    public ProgramEmitter sub(float constant) {
        return defineConstant(constant).sub();
    }

    public ProgramEmitter mul() {
        nodes.add(new EmitInstruction(Instruction.MUL));
        return this;
    }

    public ProgramEmitter mul(float constant) {
        return defineConstant(constant).mul();
    }

    public ProgramEmitter div() {
        nodes.add(new EmitInstruction(Instruction.DIV));
        return this;
    }

    public ProgramEmitter div(float constant) {
        return defineConstant(constant).div();
    }

    public ProgramEmitter mod() {
        nodes.add(new EmitInstruction(Instruction.MOD));
        return this;
    }

    public ProgramEmitter neg() {
        nodes.add(new EmitInstruction(Instruction.NEG));
        return this;
    }

    public ProgramEmitter min() {
        nodes.add(new EmitInstruction(Instruction.MIN));
        return this;
    }

    public ProgramEmitter min(float minValue) {
        return defineConstant(minValue).min();
    }

    public ProgramEmitter max() {
        nodes.add(new EmitInstruction(Instruction.MAX));
        return this;
    }

    public ProgramEmitter max(float maxValue) {
        return defineConstant(maxValue).max();
    }

    public ProgramEmitter abs() {
        nodes.add(new EmitInstruction(Instruction.ABS));
        return this;
    }

    public ProgramEmitter clamp() {
        nodes.add(new EmitInstruction(Instruction.CLAMP));
        return this;
    }

    public ProgramEmitter clamp(float minValue, float maxValue) {
        return defineConstant(minValue).defineConstant(maxValue).clamp();
    }

    public ProgramEmitter signum() {
        nodes.add(new EmitInstruction(Instruction.SIGN));
        return this;
    }

    public byte[] assemble() {
        return new byte[0];
    }

}
