package me.mdbell.awtea.impl.idb;

import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

public class IndexedDBVirtualFileAccessor implements VirtualFileAccessor {
    private final String path;
    private int position = 0; // File pointer

    public IndexedDBVirtualFileAccessor(String path, boolean append) {
        this.path = path;

        if (append) {
            this.position = size();
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = IndexedDBHelper.readFile(path, position, buffer, offset, length);
        if (read > 0) {
            position += read;
        }
        return read;
    }


    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        IndexedDBHelper.writeFile(path, position, buffer, offset, length);
        position += length;
    }

    @Override
    public int tell() throws IOException {
        return position;
    }

    public void seek(int target) throws IOException {
        if (position != target) {
            flush();
            position = target;
        }
    }

    @Override
    public void skip(int amount) throws IOException {
        int newPosition = position + amount;
        if (newPosition < 0 || newPosition > size()) {
            throw new IOException("Skip out of bounds");
        }
        position = newPosition;
    }

    @Override
    public int size() {
        return IndexedDBHelper.getFileSize(path);
    }

    @Override
    public void resize(int size) throws IOException {
        if (size < 0) {
            throw new IOException("Invalid file size");
        }
        IndexedDBHelper.resizeFile(path, size);
    }

    @Override
    public void flush() {
        // No-op (Transactions are automatically committed)
    }

    @Override
    public void close() {
        flush();
    }
}
