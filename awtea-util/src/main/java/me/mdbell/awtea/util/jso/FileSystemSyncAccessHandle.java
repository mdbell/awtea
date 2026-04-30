package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.ArrayBufferView;

public interface FileSystemSyncAccessHandle extends JSObject {

    int read(ArrayBufferView data);

    int read(ArrayBufferView data, ReadWriteOptions options);

    int write(ArrayBufferView data);

    int write(ArrayBufferView data, ReadWriteOptions options);

    int getSize();

    void flush();

    void truncate(int size);

    void close();

    interface ReadWriteOptions extends JSObject {
        @JSProperty("at")
        int getAt();

        @JSProperty("at")
        void setAt(int at);
    }
}
