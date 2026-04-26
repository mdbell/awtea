package me.mdbell.awtea.io;

import me.mdbell.awtea.util.jso.FileSystemDirectoryHandle;
import me.mdbell.awtea.util.jso.FileSystemFileHandle;
import me.mdbell.awtea.util.jso.FileSystemHandle;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.file.File;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

public class OPFSVirtualFile implements VirtualFile {

    private final FileSystemDirectoryHandle parentHandle;
    private final FileSystemHandle handle;

    public OPFSVirtualFile(FileSystemDirectoryHandle parent, FileSystemHandle handle) {
        this.parentHandle = parent;
        this.handle = handle;
    }

    private File getFile() throws IOException {
        if (!isFile()) {
            throw new IOException("Not a file");
        }
        FileSystemFileHandle fileHandle = (FileSystemFileHandle) handle;
        return fileHandle.getFile().await();
    }

    @Override
    public String getName() {
        return this.handle.getName();
    }

    @Override
    public boolean isDirectory() {
        return "directory".equals(this.handle.getKind());
    }

    @Override
    public boolean isFile() {
        return "file".equals(this.handle.getKind());
    }

    @Override
    public String[] listFiles() {
        FileSystemDirectoryHandle dirHandle = (FileSystemDirectoryHandle) handle;
        return dirHandle.listEntries();
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        return new OPFSVirtualFileAccessor((FileSystemFileHandle) this.handle);
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        if (!isDirectory()) {
            throw new IOException("Not a directory");
        }
        FileSystemDirectoryHandle dirHandle = (FileSystemDirectoryHandle) handle;
        FileSystemDirectoryHandle.GetFileSystemHandleOptions opts = JSObjects.create();
        opts.setCreate(true);
        FileSystemFileHandle newHandle = dirHandle.getFileHandle(fileName, opts).await();
        return newHandle != null;
    }

    @Override
    public boolean createDirectory(String fileName) {
        if (!isDirectory()) {
            return false;
        }
        FileSystemDirectoryHandle dirHandle = (FileSystemDirectoryHandle) handle;
        FileSystemDirectoryHandle.GetFileSystemHandleOptions opts = JSObjects.create();
        opts.setCreate(true);
        FileSystemDirectoryHandle newHandle = dirHandle.getDirectoryHandle(fileName, opts).await();
        return newHandle != null;
    }

    @Override
    public boolean delete() {
        if (parentHandle == null) {
            return false; // Cannot delete root
        }
        FileSystemDirectoryHandle.RemoveEntryOptions opts = JSObjects.create();
        opts.setRecursive(true); // Ensure directories are deleted with their contents
        try {
            parentHandle.removeEntry(handle.getName(), opts).await();
        } catch (Exception e) {
            return false; // Deletion failed (e.g. due to permissions)
        }
        return true;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        if (!isDirectory()) {
            return false;
        }
        OPFSVirtualFile source = (OPFSVirtualFile) file;
        FileSystemDirectoryHandle destDir = (FileSystemDirectoryHandle) this.handle;

        // 1. Try the efficient move first
        try {
            source.handle.move(destDir).await();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public long lastModified() {
        try {
            return (long) getFile().getLastModified();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return getFile().getSize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
