package me.mdbell.awtea.io;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

public class ReadAheadVirtualFile implements VirtualFile {

    private final VirtualFile delegate;
    private final int readAheadSize;
    private final int writeBufferSize;

    public ReadAheadVirtualFile(VirtualFile delegate, int readAheadSize, int writeBufferSize) {
        this.delegate = delegate;
        this.readAheadSize = readAheadSize;
        this.writeBufferSize = writeBufferSize;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isDirectory() {
        return delegate.isDirectory();
    }

    @Override
    public boolean isFile() {
        return delegate.isFile();
    }

    @Override
    public String[] listFiles() {
        return delegate.listFiles();
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        return new ReadAheadVirtualFileAccessor(delegate.createAccessor(readable, writable, append), readAheadSize,
                writeBufferSize);
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        return delegate.createFile(fileName);
    }

    @Override
    public boolean createDirectory(String fileName) {
        return delegate.createDirectory(fileName);
    }

    @Override
    public boolean delete() {
        return delegate.delete();
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        VirtualFile source = file;
        if (file instanceof ReadAheadVirtualFile) {
            source = ((ReadAheadVirtualFile) file).delegate;
        }
        return delegate.adopt(source, fileName);
    }

    @Override
    public boolean canRead() {
        return delegate.canRead();
    }

    @Override
    public boolean canWrite() {
        return delegate.canWrite();
    }

    @Override
    public long lastModified() {
        return delegate.lastModified();
    }

    @Override
    public boolean setLastModified(long lastModified) {
        return delegate.setLastModified(lastModified);
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        return delegate.setReadOnly(readOnly);
    }

    @Override
    public int length() {
        return delegate.length();
    }
}
