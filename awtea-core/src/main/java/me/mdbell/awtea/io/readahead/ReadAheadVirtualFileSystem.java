package me.mdbell.awtea.io.readahead;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

public class ReadAheadVirtualFileSystem implements VirtualFileSystem {

    private final VirtualFileSystem delegate;
    private final int readAheadSize;
    private final int writeBufferSize;

    public ReadAheadVirtualFileSystem(VirtualFileSystem delegate) {
        this(delegate, 64 * 1024);
    }

    public ReadAheadVirtualFileSystem(VirtualFileSystem delegate, int readAheadSize) {
        this(delegate, readAheadSize, 0);
    }

    public ReadAheadVirtualFileSystem(VirtualFileSystem delegate, int readAheadSize, int writeBufferSize) {
        this.delegate = delegate;
        this.readAheadSize = Math.max(1024, readAheadSize);
        this.writeBufferSize = Math.max(0, writeBufferSize);
    }

    @Override
    public String getUserDir() {
        return delegate.getUserDir();
    }

    @Override
    public VirtualFile getFile(String path) {
        VirtualFile file = delegate.getFile(path);
        if (file == null) {
            return null;
        }
        return new ReadAheadVirtualFile(file, readAheadSize, writeBufferSize);
    }

    @Override
    public boolean isWindows() {
        return delegate.isWindows();
    }

    @Override
    public String canonicalize(String path) {
        return delegate.canonicalize(path);
    }

    @Override
    public String[] getRoots() {
        return delegate.getRoots();
    }
}
