package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

public abstract class FileSystemHandle implements JSObject {

    @JSProperty("kind")
    public native String getKind();

    @JSProperty("name")
    public native String getName();

    public native boolean isSameEntry(FileSystemHandle other);

    public native JSPromise<JSUndefined> move(String newName);

    public native JSPromise<JSUndefined> move(FileSystemDirectoryHandle newParent);

    public native JSPromise<JSUndefined> move(FileSystemDirectoryHandle newParent, String newName);

    // non baseline methods like queryPermission, requestPermission, etc. are not included for now
}
