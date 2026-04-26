package me.mdbell.awtea.io;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.jso.FileSystemFileHandle;
import me.mdbell.awtea.util.jso.FileSystemWritableFileStream;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.file.Blob;
import org.teavm.jso.file.File;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

@ExtensionMethod({ JSObjectsExtensions.class })
class OPFSVirtualFileAccessor implements VirtualFileAccessor {

    private final FileSystemFileHandle fileHandle;
    private int position = 0;

    private static final FileSystemFileHandle.CreateWritableOptions writeOptions = JSObjects.create();

    static {
        writeOptions.setKeepExistingData(true);
    }

    OPFSVirtualFileAccessor(FileSystemFileHandle file) {
        this.fileHandle = file;
    }

    @Override
    public int read(byte[] buffer, int offset, int limit) throws IOException {
        // 1. Get a snapshot of the file
        File file = fileHandle.getFile().await();
        int fileSize = (int) file.getSize();

        if (position >= fileSize)
            return -1;

        // 2. Slice the file to get only the bytes we need
        int end = Math.min(position + limit, fileSize);
        Blob slice = file.slice(position, end);

        Int8Array nativeArray = Int8Array.fromJavaArray(buffer);
        ArrayBuffer data = slice.arrayBuffer().await();

        Int8Array sliceArray = new Int8Array(data);
        nativeArray.set(sliceArray, offset);

        int bytesRead = end - position;
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public void write(byte[] buffer, int offset, int limit) throws IOException {
        // WARNING: On the main thread, 'createWritable' usually wipes the file
        // or creates a temp file. For true random access, we have to be careful.

        FileSystemWritableFileStream writable = fileHandle.createWritable(writeOptions).await();

        // Seek to the current position in the stream
        writable.seek(position).await();

        Int8Array nativeArray = Int8Array.fromJavaArray(buffer);

        Uint8Array sliceArray = new Uint8Array(nativeArray.getBuffer(), nativeArray.getByteOffset() + offset, limit);

        // Write the chunk
        writable.write(sliceArray).await();

        // In OPFS main thread, you MUST close to persist
        writable.close().await();

        position += limit;
    }

    @Override
    public int tell() throws IOException {
        return position;
    }

    @Override
    public void seek(int target) throws IOException {
        this.position = target;
    }

    @Override
    public void skip(int amount) throws IOException {
        this.position += amount;
    }

    @Override
    public int size() throws IOException {
        return (int) fileHandle.getFile().await().getSize();
    }

    @Override
    public void resize(int size) throws IOException {
        FileSystemWritableFileStream writable = fileHandle.createWritable(writeOptions).await();
        writable.truncate(size).await();
        writable.close().await();
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void flush() throws IOException {

    }
}
