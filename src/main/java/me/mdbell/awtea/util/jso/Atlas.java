package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;

public interface Atlas extends JSObject {
    @JSIndexer
    Glyph get(String key);
}
