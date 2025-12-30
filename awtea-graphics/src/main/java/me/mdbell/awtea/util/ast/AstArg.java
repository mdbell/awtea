package me.mdbell.awtea.util.ast;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class AstArg {
    private final String text;
    private final Float value;
}
