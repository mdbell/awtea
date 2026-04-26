package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

public interface FileSystemDirectoryHandle extends FileSystemHandle {

    JSPromise<FileSystemDirectoryHandle> getDirectoryHandle(String name);

    JSPromise<FileSystemDirectoryHandle> getDirectoryHandle(String name, GetFileSystemHandleOptions options);

    JSPromise<FileSystemFileHandle> getFileHandle(String name);

    JSPromise<FileSystemFileHandle> getFileHandle(String name, GetFileSystemHandleOptions options);

    JSPromise<JSUndefined> removeEntry(String name, RemoveEntryOptions options);

    JSPromise<String[]> resolve(FileSystemHandle possibleDescendant);

    //TODO: entries(), keys(), values(), etc. are not included for now, as they require async iterators which are not (?) supported by TeaVM

    public interface GetFileSystemHandleOptions extends JSObject {
        @JSProperty("create")
        boolean isCreate();

        @JSProperty("create")
        void setCreate(boolean create);
    }

    public interface RemoveEntryOptions extends JSObject {
        @JSProperty("recursive")
        boolean isRecursive();

        @JSProperty("recursive")
        void setRecursive(boolean recursive);
    }
}
