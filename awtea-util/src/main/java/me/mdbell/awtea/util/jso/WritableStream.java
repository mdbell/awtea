package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

@JSClass
public class WritableStream implements JSObject {

    public WritableStream() {
        
    }

    @JSProperty("locked")
    public native boolean isLocked();

    public native JSPromise<JSUndefined> abort();

    public native JSPromise<JSUndefined> close();

    public native WritableStreamDefaultWriter getWriter();
}
