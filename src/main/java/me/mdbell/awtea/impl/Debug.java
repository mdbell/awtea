package me.mdbell.awtea.impl;

import org.teavm.jso.JSBody;

public class Debug {

    private Debug() {

    }

    public static UnsupportedOperationException unimplemented() {
        trigger();
        return new UnsupportedOperationException("Unimplemented");
    }

    @JSBody(script = "debugger;")
    public static native void trigger();
}
