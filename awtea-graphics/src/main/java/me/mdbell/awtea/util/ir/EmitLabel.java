package me.mdbell.awtea.util.ir;

import lombok.Value;

@Value
public class EmitLabel implements EmitNode {
    private final String name;
}
