package me.mdbell.awtea.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Value;

public class ShaderTokenizer implements Iterator<ShaderTokenizer.Token> {

    public enum TokenType {
        KEYWORD, DIRECTIVE, LABEL, IDENT, NUMBER, COLON, COMMA, COMMENT, NEWLINE, EOF
    }

    @Value
    @Builder
    public static class Token {
        private TokenType type;
        private String text;
        private int line, col;
        private int value;

        @Override
        public String toString() {
            if (type == TokenType.NUMBER)
                return String.format("(%s,'%s'=%d @%d:%d)", type, text, value, line, col);
            return String.format("(%s,'%s' @%d:%d)", type, text, line, col);
        }
    }

    // opcodes (temp set/array, will be made more dynamic later)
    static final Set<String> MNEMONICS = new HashSet<>(
            Arrays.asList(Instruction.values())
                    .stream().map(Instruction::toString).collect(Collectors.toList()));

    private BufferedReader reader;
    private String line;
    private int lineNo = 1, colNo = 0, cursor = 0;
    boolean done = false;

    Token peeked = null;

    public ShaderTokenizer(File src) throws IOException {
        this(new FileInputStream(src));
    }

    public ShaderTokenizer(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        this.reader = new BufferedReader(isr);
        advanceLine();
    }

    @Override
    public boolean hasNext() {
        if (peeked == null) {
            try {
                peeked = nextToken();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return peeked != null && peeked.type != TokenType.EOF;
    }

    @Override
    public Token next() {
        if (peeked != null) {
            Token t = peeked;
            peeked = null;
            return t;
        }
        try {
            return nextToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isKeyword(String s) {
        return MNEMONICS.contains(s);
    }

    void advanceLine() throws IOException {
        line = reader.readLine();
        cursor = colNo = 0;
    }

    private Token newLine(int line) {
        return Token.builder()
                .type(TokenType.NEWLINE)
                .text("\\n")
                .line(line)
                .build();
    }

    private Token comment(String comment, int line, int col) {
        return Token.builder()
                .type(TokenType.COMMENT)
                .text(comment)
                .line(line)
                .col(col)
                .build();
    }

    private Token colon(int line, int col) {
        return Token.builder()
                .type(TokenType.COLON)
                .text(":")
                .line(line)
                .col(col)
                .build();
    }

    private Token comma(int line, int col) {
        return Token.builder()
                .type(TokenType.COMMA)
                .text(",")
                .line(line)
                .col(col)
                .build();
    }

    private Token label(String name, int line, int col) {
        return Token.builder()
                .type(TokenType.LABEL)
                .text(name)
                .line(line)
                .col(col)
                .build();
    }

    private Token directive(String dir, int line, int col) {
        return Token.builder()
                .type(TokenType.DIRECTIVE)
                .text(dir)
                .line(line)
                .col(col)
                .build();
    }

    private Token number(String raw, int line, int col, int value) {
        return Token.builder()
                .type(TokenType.NUMBER)
                .text(raw)
                .line(line)
                .col(col)
                .value(value)
                .build();
    }

    private Token ident(String str, int line, int col) {
        return Token.builder()
                .type(TokenType.IDENT)
                .text(str)
                .line(line)
                .col(col)
                .build();
    }

    private Token keyword(String word, int line, int col) {
        return Token.builder()
                .type(TokenType.KEYWORD)
                .text(word)
                .line(line)
                .col(col)
                .build();
    }

    private Token eof(int line) {
        return Token.builder()
                .type(TokenType.EOF)
                .text("")
                .line(line)
                .build();
    }

    private Token nextToken() throws IOException {
        while (line != null) {
            if (cursor >= line.length()) { // End of line
                advanceLine();
                lineNo++;
                return newLine(lineNo);
            }
            // Skip leading whitespace
            while (cursor < line.length() && Character.isWhitespace(line.charAt(cursor))) {
                cursor++;
                colNo++;
            }
            if (cursor >= line.length())
                continue; // Blank: will emit NEWLINE above

            int startCol = colNo;
            char ch = line.charAt(cursor);

            // Comments
            if (ch == ';' || ch == '#') {
                String txt = line.substring(cursor);
                cursor = line.length();
                return comment(txt, lineNo, startCol);
            }
            if (ch == '/' && cursor + 1 < line.length() && line.charAt(cursor + 1) == '/') {
                String txt = line.substring(cursor);
                cursor = line.length();
                return comment(txt, lineNo, startCol);
            }

            // Labels
            int colonIdx = line.indexOf(':', cursor);
            if (colonIdx == cursor) {
                cursor++;
                colNo++;
                return colon(lineNo, startCol);
            } else if (colonIdx > cursor) {
                String before = line.substring(cursor, colonIdx).trim();
                if (!before.isEmpty() && before.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    Token t1 = label(before, lineNo, startCol);
                    cursor = colonIdx;
                    colNo += before.length();
                    // COLON will be handled on next iteration
                    return t1;
                }
            }

            // Directives
            if (ch == '.') {
                int end = cursor + 1;
                while (end < line.length() && Character.isLetter(line.charAt(end)))
                    end++;
                String dir = line.substring(cursor, end);
                cursor = end;
                colNo += dir.length();
                return directive(dir, lineNo, startCol);
            }

            // Punctuation
            if (ch == ',') {
                cursor++;
                colNo++;
                return comma(lineNo, startCol);
            }

            // Number: decimal, hex, or float -> always NUMBER() with int value
            if (Character.isDigit(ch) || ch == '-' || ch == '+') {
                int end = cursor + 1;
                boolean seenDot = false, seenX = false, seenE = false;
                while (end < line.length()) {
                    char c = line.charAt(end);
                    if (Character.isDigit(c)) {
                    } else if (c == '.' && !seenDot) {
                        seenDot = true;
                    } else if ((c == 'x' || c == 'X') && !seenX) {
                        seenX = true;
                    } else if ((c == 'e' || c == 'E') && !seenE) {
                        seenE = true;
                    } else if (c == '-' || c == '+') {
                    } else
                        break;
                    end++;
                }
                String num = line.substring(cursor, end);
                cursor = end;
                colNo += num.length();

                int val = 0;
                try {
                    if (num.matches("[-+]?0[xX][0-9a-fA-F]+")) {
                        // strip out the 0x prefix
                        num = num.substring(2);
                        long asInt = Long.parseLong(num.replace("_", ""), 16);
                        val = (int) (asInt << 16);
                    } else if (num.matches("[-+]?[0-9]*\\.[0-9]+([eE][-+]?[0-9]+)?")) {
                        double d = Double.parseDouble(num);
                        val = (int) Math.round(d * 65536.0);
                    } else if (num.matches("[-+]?[0-9]+")) {
                        long asInt = Long.parseLong(num.replace("_", ""));
                        val = (int) (asInt << 16);
                    } else {
                        return ident(num, lineNo, startCol);
                    }
                    return number(num, lineNo, startCol, val);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return ident(num, lineNo, startCol);
                }
            }

            // Directive or Keyword or Ident (word)
            if (Character.isLetter(ch) || ch == '_') {
                int end = cursor + 1;
                while (end < line.length() &&
                        (Character.isLetterOrDigit(line.charAt(end)) || line.charAt(end) == '_'))
                    end++;
                String word = line.substring(cursor, end);
                cursor = end;
                colNo += word.length();

                if (isKeyword(word)) {
                    return keyword(word, lineNo, startCol);
                } else {
                    return ident(word, lineNo, startCol);
                }
            }

            // If unrecognized, skip and continue
            cursor++;
            colNo++;
        }
        if (!done) {
            done = true;
            return eof(lineNo);
        }
        return null;
    }
}
