package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.file.File;

public class FileSystemFileHandle extends FileSystemHandle {

    public native JSPromise<File> getFile();

    public native JSPromise<FileSystemSyncAccessHandle> createSyncAccessHandle();

    public native JSPromise<FileSystemWritableFileStream> createWritable();

    public native JSPromise<FileSystemWritableFileStream> createWritable(CreateWritableOptions options);

    public interface CreateWritableOptions extends JSObject {
        @JSProperty("keepExistingData")
        boolean isKeepExistingData();

        @JSProperty("keepExistingData")
        void setKeepExistingData(boolean keepExistingData);

        // Also has "mode", but it's not baseline
    }
}
