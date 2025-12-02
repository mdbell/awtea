package me.mdbell.awtea.impl.idb;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

public class IndexedDBVirtualFile implements VirtualFile {
    private String path;

    private int position = 0; // File pointer

    public IndexedDBVirtualFile(String path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    @Override
    public boolean isDirectory() {
        return IndexedDBHelper.isDirectory(path);
    }

    @Override
    public boolean isFile() {
        return IndexedDBHelper.isFile(path);
    }

    @Override
    public String[] listFiles() {
        return IndexedDBHelper.listDirectory(path);
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        return new CachingVirtualFileAccessor(new IndexedDBVirtualFileAccessor(path, append));
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder(path);
        if (!path.endsWith("/") && !fileName.startsWith("/")) {
            sb.append("/");
        }
        sb.append(fileName);
        return IndexedDBHelper.createFile(sb.toString());
    }

    @Override
    public boolean createDirectory(String fileName) {
        StringBuilder sb = new StringBuilder(path);
        if (!path.endsWith("/") && !fileName.startsWith("/")) {
            sb.append("/");
        }
        sb.append(fileName);

        return IndexedDBHelper.createDirectory(sb.toString());
    }

    @Override
    public boolean delete() {
        return IndexedDBHelper.deleteFile(path);
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        return IndexedDBHelper.moveFile(file.getName(), path + "/" + fileName);
    }

    @Override
    public boolean canRead() {
        return true; // Assume readable
    }

    @Override
    public boolean canWrite() {
        return !IndexedDBHelper.isReadOnly(path);
    }

    @Override
    public long lastModified() {
        return IndexedDBHelper.getLastModified(path);
    }

    @Override
    public boolean setLastModified(long lastModified) {
        return IndexedDBHelper.setLastModified(path, lastModified);
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        return IndexedDBHelper.setReadOnly(path, readOnly);
    }

    @Override
    public int length() {
        return IndexedDBHelper.getFileSize(path);
    }
}
