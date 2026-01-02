package me.mdbell.awtea.util.ir;

import lombok.Value;

@Value
public class EmitDefine implements EmitNode {
    private final Type type;
    /**
     * The name of the define. If null it is considered anonymous, and the
     * compiler will combine consts with the same value.
     */
    private final String name;
    private final float value;

    public enum Type {
        UNIFORM, CONST, INPUT;
    }
}
