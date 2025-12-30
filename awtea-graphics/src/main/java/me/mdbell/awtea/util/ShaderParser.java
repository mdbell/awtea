package me.mdbell.awtea.util;

import java.util.ArrayList;
import java.util.List;

import me.mdbell.awtea.util.ShaderTokenizer.Token;
import me.mdbell.awtea.util.ast.AstArg;
import me.mdbell.awtea.util.ast.AstNode;
import me.mdbell.awtea.util.ast.ConstNode;
import me.mdbell.awtea.util.ast.DirectiveNode;
import me.mdbell.awtea.util.ast.InstructionNode;
import me.mdbell.awtea.util.ast.LabelNode;
import me.mdbell.awtea.util.ast.ProgramNode;

public class ShaderParser {

    private final ShaderTokenizer tokenizer;
    private ShaderTokenizer.Token lookahead;

    public ShaderParser(ShaderTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.lookahead = tokenizer.hasNext() ? tokenizer.next() : null;
    }

    private ShaderTokenizer.Token peek() {
        return lookahead;
    }

    // Consume and advance; ensure it's of expected type, else throw
    private ShaderTokenizer.Token expect(ShaderTokenizer.TokenType expected) {
        if (lookahead == null)
            throw new RuntimeException("Unexpected end of input, expected " + expected);
        if (lookahead.getType() != expected)
            throw new RuntimeException("Expected " + expected + " but got " + lookahead.getType() + " ("
                    + lookahead.getText() + ") at line " + lookahead.getLine());
        ShaderTokenizer.Token ret = lookahead;
        lookahead = tokenizer.hasNext() ? tokenizer.next() : null;
        return ret;
    }

    // Helper: advance any newlines or comments
    private void skipTrivia() {
        while (lookahead != null &&
                (lookahead.getType() == ShaderTokenizer.TokenType.NEWLINE ||
                        lookahead.getType() == ShaderTokenizer.TokenType.COMMENT)) {
            lookahead = tokenizer.hasNext() ? tokenizer.next() : null;
        }
    }

    // Parse entire program
    public ProgramNode parse() {
        List<AstNode> stmts = new ArrayList<>();
        skipTrivia();
        while (lookahead != null && lookahead.getType() != ShaderTokenizer.TokenType.EOF) {
            stmts.add(parseStatement());
            skipTrivia();
        }
        return new ProgramNode(stmts);
    }

    public AstNode parseStatement() {
        skipTrivia();
        Token start = lookahead;
        Token end = start;
        if (lookahead == null || lookahead.getType() == ShaderTokenizer.TokenType.EOF)
            return null;
        switch (lookahead.getType()) {
            case LABEL:
                String label = expect(ShaderTokenizer.TokenType.LABEL).getText();
                end = expect(ShaderTokenizer.TokenType.COLON);
                return new LabelNode(start, end, label);
            case DIRECTIVE:
                String dir = expect(ShaderTokenizer.TokenType.DIRECTIVE).getText();
                if (".const".equalsIgnoreCase(dir)) {
                    // Parse .const NAME VALUE
                    Token nameTk = expect(ShaderTokenizer.TokenType.IDENT);
                    String name = nameTk.getText();
                    Token valueTk = expect(ShaderTokenizer.TokenType.NUMBER);
                    int value = valueTk.getValue();
                    end = valueTk;
                    return new ConstNode(start, end, name, value);
                }
                List<String> dargs = new ArrayList<>();
                // Count args until newline or comment or EOF
                while (lookahead != null &&
                        (lookahead.getType() == ShaderTokenizer.TokenType.IDENT
                                || lookahead.getType() == ShaderTokenizer.TokenType.NUMBER)) {
                    ShaderTokenizer.Token t = lookahead;
                    dargs.add(t.getText());
                    end = lookahead;
                    lookahead = tokenizer.hasNext() ? tokenizer.next() : null;
                }
                return new DirectiveNode(start, end, dir, dargs);
            case KEYWORD:
                String mnemonic = expect(ShaderTokenizer.TokenType.KEYWORD).getText();
                List<AstArg> iargs = new ArrayList<>();
                // Count args until newline/EOF/comment/comma (can support comma separation)
                while (lookahead != null &&
                        (lookahead.getType() == ShaderTokenizer.TokenType.IDENT
                                || lookahead.getType() == ShaderTokenizer.TokenType.NUMBER
                                || lookahead.getType() == ShaderTokenizer.TokenType.COMMA)) {
                    if (lookahead.getType() == ShaderTokenizer.TokenType.COMMA) {
                        lookahead = tokenizer.hasNext() ? tokenizer.next() : null;
                        continue;
                    }
                    ShaderTokenizer.Token t = lookahead;
                    Integer value = t.getType() == ShaderTokenizer.TokenType.NUMBER ? t.getValue() : null;
                    iargs.add(new AstArg(t.getText(), value));
                    end = lookahead;
                    lookahead = tokenizer.hasNext() ? tokenizer.next() : null;
                }
                Instruction instruction = Instruction.valueOf(mnemonic);
                return new InstructionNode(start, end, instruction, iargs);
            default:
                throw new RuntimeException("Unknown/unsupported statement start: " + lookahead.getType() + " at line "
                        + lookahead.getLine());
        }
    }
}
