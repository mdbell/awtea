package me.mdbell.awtea.io.mount;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;

import java.io.IOException;

class MountPointVirtualFile implements VirtualFile {

    private final String mountPath;
    private final VirtualFile delegateRoot;

    MountPointVirtualFile(String mountPath, VirtualFileSystem fileSystem) {
        this.mountPath = mountPath;
        this.delegateRoot = fileSystem.getFile(fileSystem.getUserDir());
    }

    @Override
    public String getName() {
        return MountVirtualFileSystem.lastSegment(mountPath);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public String[] listFiles() {
        if (delegateRoot == null) {
            return new String[0];
        }
        String[] files = delegateRoot.listFiles();
        return files != null ? files : new String[0];
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        throw new UnsupportedOperationException("Cannot open accessor for mount point directory");
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        return delegateRoot != null && delegateRoot.createFile(fileName);
    }

    @Override
    public boolean createDirectory(String fileName) {
        return delegateRoot != null && delegateRoot.createDirectory(fileName);
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        return delegateRoot != null && delegateRoot.adopt(file, fileName);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return delegateRoot != null && delegateRoot.canWrite();
    }

    @Override
    public long lastModified() {
        return delegateRoot != null ? delegateRoot.lastModified() : 0;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        return delegateRoot != null && delegateRoot.setLastModified(lastModified);
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        return delegateRoot != null && delegateRoot.setReadOnly(readOnly);
    }

    @Override
    public int length() {
        return delegateRoot != null ? delegateRoot.length() : 0;
    }
}
