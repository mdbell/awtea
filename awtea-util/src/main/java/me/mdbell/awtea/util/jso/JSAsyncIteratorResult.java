package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface JSAsyncIteratorResult<T extends JSObject> extends JSObject {

    @JSProperty("done")
    boolean isDone();

    @JSProperty("value")
    T getValue();
}
