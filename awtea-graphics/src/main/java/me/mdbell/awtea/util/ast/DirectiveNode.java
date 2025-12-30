package me.mdbell.awtea.util.ast;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.util.ShaderTokenizer.Token;

@Getter
@ToString
public class DirectiveNode extends AstNode {
    private final String name;
    private final List<String> args;

    public DirectiveNode(Token start, Token end, String name, List<String> args) {
        super(start, end);
        this.name = name;
        this.args = new LinkedList<>(args);
    }
}
