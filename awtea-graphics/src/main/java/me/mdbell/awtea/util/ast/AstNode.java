package me.mdbell.awtea.util.ast;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.util.ShaderTokenizer.Token;

@RequiredArgsConstructor
@Getter
public abstract class AstNode {
    private final Token start;
    private final Token end;
}
