package me.mdbell.awtea.util.ast;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.util.Instruction;
import me.mdbell.awtea.util.ShaderTokenizer.Token;

@Getter
@ToString
public class InstructionNode extends AstNode {
    private final Instruction insn;
    private final List<AstArg> args;

    public InstructionNode(Token start, Token end, Instruction insn, List<AstArg> args) {
        super(start, end);
        this.insn = insn;
        this.args = new LinkedList<>(args);
    }
}
