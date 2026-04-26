package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

public interface JSAsyncIterator<T extends JSObject> extends JSObject {

    JSPromise<JSAsyncIteratorResult<T>> next();
}
