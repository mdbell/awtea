package me.mdbell.awtea.util.ast;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.util.ShaderTokenizer.Token;

@Getter
@ToString
public class ConstNode extends AstNode {
    private final String name;
    private final float value;

    public ConstNode(Token start, Token end, String name, float value) {
        super(start, end);
        this.name = name;
        this.value = value;
    }
}
