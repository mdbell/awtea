package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.Uint8Array;

public interface WritableStreamDefaultWriter extends JSObject {

    @JSProperty("closed")
    boolean isClosed();

    @JSProperty("desiredSize")
    double getDesiredSize();

    @JSProperty("ready")
    JSPromise<JSBoolean> getReady();

    JSPromise<JSUndefined> abort();

    JSPromise<JSUndefined> abort(String reason);

    JSPromise<JSUndefined> close();

    void releaseLock();

    JSPromise<JSUndefined> write(Uint8Array arr);
}
