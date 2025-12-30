package me.mdbell.awtea.util.ast;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.util.ShaderTokenizer.Token;

@Getter
@ToString
public class LabelNode extends AstNode {
    private final String name;

    public LabelNode(Token start, Token end, String name) {
        super(start, end);
        this.name = name;
    }
}
