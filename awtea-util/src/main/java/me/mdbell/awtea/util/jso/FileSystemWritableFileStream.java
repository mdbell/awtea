package me.mdbell.awtea.util.jso;

import org.teavm.common.binary.Blob;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.TypedArray;

public abstract class FileSystemWritableFileStream extends WritableStream {

    public native JSPromise<JSUndefined> write(ArrayBuffer data);

    public native JSPromise<JSUndefined> write(TypedArray data);

    public native JSPromise<JSUndefined> write(DataView data);

    public native JSPromise<JSUndefined> write(Blob data);

    public native JSPromise<JSUndefined> write(String data);

    public native JSPromise<JSUndefined> write(WriteObject data);

    public native JSPromise<JSUndefined> seek(double position);

    public native JSPromise<JSUndefined> truncate(double size);

    public interface WriteObject extends JSObject {

        @JSProperty("type")
        String getType();

        @JSProperty("type")
        void setType(String type);

        @JSProperty("data")
        JSObject getData();

        @JSProperty("data")
        void setData(JSObject data);

        @JSProperty("position")
        double getPosition();

        @JSProperty("position")
        void setPosition(double position);

        @JSProperty("size")
        double getSize();

        @JSProperty("size")
        void setSize(double size);
    }
}
