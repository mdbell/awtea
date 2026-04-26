package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.core.JSUndefined;

public class FileSystemDirectoryHandle extends FileSystemHandle {

    public native JSPromise<FileSystemDirectoryHandle> getDirectoryHandle(String name);

    public native JSPromise<FileSystemDirectoryHandle> getDirectoryHandle(String name, GetFileSystemHandleOptions options);

    public native JSPromise<FileSystemFileHandle> getFileHandle(String name);

    public native JSPromise<FileSystemFileHandle> getFileHandle(String name, GetFileSystemHandleOptions options);

    public native JSPromise<JSUndefined> removeEntry(String name, RemoveEntryOptions options);

    public native JSPromise<String[]> resolve(FileSystemHandle possibleDescendant);

    //TODO: entries(), keys(), values(), etc. are not included for now, as they require async iterators which are not (?) supported by TeaVM

    public String[] listEntries() {
        JSArray<JSString> keys = keys(this).await();
        String[] result = new String[keys.getLength()];
        for (int i = 0; i < keys.getLength(); i++) {
            result[i] = keys.get(i).stringValue();
        }
        return result;
    }

    @JSBody(params = {"dir"}, script = "return Array.fromAsync(dir.keys());")
    private static native JSPromise<JSArray<JSString>> keys(FileSystemDirectoryHandle handle);

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
