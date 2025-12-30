package me.mdbell.awtea.util.ast;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ProgramNode extends AstNode {
    private final List<AstNode> statements;

    public ProgramNode(List<AstNode> statements) {
        super(statements.get(0).getStart(), statements.get(statements.size() - 1).getEnd());
        this.statements = new LinkedList<>(statements);
    }
}
