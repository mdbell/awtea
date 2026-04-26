package me.mdbell.awtea.io.mount;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

class MountRootVirtualFile implements VirtualFile {

    private final MountVirtualFileSystem owner;

    MountRootVirtualFile(MountVirtualFileSystem owner) {
        this.owner = owner;
    }

    @Override
    public String getName() {
        return MountVirtualFileSystem.ROOT;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    private VirtualFile baseRoot() {
        if (owner.baseFileSystem == null) {
            return null;
        }
        return owner.baseFileSystem.getFile(owner.baseFileSystem.getUserDir());
    }

    @Override
    public String[] listFiles() {
        return owner.listRootFiles();
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        VirtualFile base = baseRoot();
        if (base != null) {
            return base.createAccessor(readable, writable, append);
        }
        throw new UnsupportedOperationException("Cannot open accessor for mount root: no base filesystem mounted");
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        VirtualFile base = baseRoot();
        return base != null && base.createFile(fileName);
    }

    @Override
    public boolean createDirectory(String fileName) {
        VirtualFile base = baseRoot();
        return base != null && base.createDirectory(fileName);
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        VirtualFile base = baseRoot();
        return base != null && base.adopt(file, fileName);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        VirtualFile base = baseRoot();
        return base != null && base.canWrite();
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        return false;
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        return false;
    }

    @Override
    public int length() {
        return 0;
    }
}
