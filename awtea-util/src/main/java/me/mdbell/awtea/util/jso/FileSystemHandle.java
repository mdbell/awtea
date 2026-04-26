package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

public interface FileSystemHandle extends JSObject {

    @JSProperty("kind")
    String getKind();

    @JSProperty("name")
    String getName();

    boolean isSameEntry(FileSystemHandle other);

    JSPromise<JSUndefined> move(String newName);

    JSPromise<JSUndefined> move(FileSystemDirectoryHandle newParent);

    JSPromise<JSUndefined> move(FileSystemDirectoryHandle newParent, String newName);

    // non baseline methods like queryPermission, requestPermission, etc. are not included for now
}
