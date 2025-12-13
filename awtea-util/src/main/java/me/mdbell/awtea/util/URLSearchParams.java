package me.mdbell.awtea.util;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

@JSClass(name = "URLSearchParams")
public class URLSearchParams implements JSObject {

    public URLSearchParams(String str) {

    }

    @JSProperty
    public native int getSize();

    public native void append(String name, String value);

    public native void delete(String name);

    public native String get(String name);

    public native String[] getAll(String name);

    public native boolean has(String name);
}
