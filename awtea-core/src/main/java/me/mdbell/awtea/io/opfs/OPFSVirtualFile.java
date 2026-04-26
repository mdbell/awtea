package me.mdbell.awtea.io.opfs;

import me.mdbell.awtea.util.jso.FileSystemDirectoryHandle;
import me.mdbell.awtea.util.jso.FileSystemFileHandle;
import me.mdbell.awtea.util.jso.FileSystemHandle;
import me.mdbell.awtea.util.jso.JSAsyncIterators;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.file.File;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OPFSVirtualFile implements VirtualFile {

    private final FileSystemDirectoryHandle parentHandle;
    private final String name;
    private final FileSystemDirectoryHandle rootDirectoryHandle;
    private FileSystemDirectoryHandle directoryHandle;
    private FileSystemFileHandle fileHandle;

    private static final FileSystemDirectoryHandle.GetFileSystemHandleOptions noCreateOptions = JSObjects.create();

    static {
        noCreateOptions.setCreate(false);
    }

    public OPFSVirtualFile(FileSystemDirectoryHandle parentHandle, String name) {
        this(parentHandle, name, null);
    }

    private OPFSVirtualFile(FileSystemDirectoryHandle parentHandle, String name,
                            FileSystemDirectoryHandle rootDirectoryHandle) {
        this.parentHandle = parentHandle;
        this.name = name;
        this.rootDirectoryHandle = rootDirectoryHandle;
    }

    public static OPFSVirtualFile root(FileSystemDirectoryHandle rootHandle) {
        return new OPFSVirtualFile(null, "/", rootHandle);
    }

    private boolean isRoot() {
        return rootDirectoryHandle != null;
    }

    private FileSystemDirectoryHandle getExistingDirectoryHandle() {
        if (isRoot()) {
            return rootDirectoryHandle;
        }
        if (directoryHandle != null) {
            return directoryHandle;
        }
        if (parentHandle == null || name == null || name.isEmpty()) {
            return null;
        }
        try {
            directoryHandle = parentHandle.getDirectoryHandle(name, noCreateOptions).await();
            return directoryHandle;
        } catch (Exception ignored) {
            return null;
        }
    }

    private FileSystemFileHandle getExistingFileHandle() {
        if (isRoot()) {
            return null;
        }
        if (fileHandle != null) {
            return fileHandle;
        }
        if (parentHandle == null || name == null || name.isEmpty()) {
            return null;
        }
        try {
            fileHandle = parentHandle.getFileHandle(name, noCreateOptions).await();
            return fileHandle;
        } catch (Exception ignored) {
            return null;
        }
    }

    private FileSystemFileHandle getOrCreateFileHandle() throws IOException {
        FileSystemFileHandle existing = getExistingFileHandle();
        if (existing != null) {
            return existing;
        }
        if (isRoot() || parentHandle == null || name == null || name.isEmpty()) {
            throw new IOException("Not a file");
        }

        FileSystemDirectoryHandle.GetFileSystemHandleOptions opts = JSObjects.create();
        opts.setCreate(true);
        try {
            fileHandle = parentHandle.getFileHandle(name, opts).await();
            return fileHandle;
        } catch (Exception e) {
            throw new IOException("Could not create file '" + name + "'", e);
        }
    }

    private FileSystemHandle getExistingHandle() {
        FileSystemDirectoryHandle existingDir = getExistingDirectoryHandle();
        if (existingDir != null) {
            return existingDir;
        }
        return getExistingFileHandle();
    }

    private File getFile() throws IOException {
        FileSystemFileHandle existing = getExistingFileHandle();
        if (existing == null) {
            throw new IOException("Not a file");
        }
        return existing.getFile().await();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDirectory() {
        return getExistingDirectoryHandle() != null;
    }

    @Override
    public boolean isFile() {
        return getExistingFileHandle() != null;
    }

    @Override
    public String[] listFiles() {
        FileSystemDirectoryHandle dirHandle = getExistingDirectoryHandle();
        if (dirHandle == null) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        for (String key : JSAsyncIterators.map(dirHandle.keys(), JSString::stringValue)) {
            result.add(key);
        }
        return result.toArray(new String[0]);
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        try {
            FileSystemFileHandle handle = writable ? getOrCreateFileHandle() : getExistingFileHandle();
            if (handle == null) {
                throw new IOException("Not a file: " + name);
            }
            if (OPFSWorkerVirtualFileAccessor.isEnabled() &&
                    OPFSWorkerVirtualFileAccessor.isSupported(handle)) {
                return new OPFSWorkerVirtualFileAccessor(handle);
            }
            return new OPFSVirtualFileAccessor(handle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        System.out.println("OPFSVirtualFile.createFile: " + fileName);
        FileSystemDirectoryHandle dirHandle = getExistingDirectoryHandle();
        if (dirHandle == null) {
            throw new IOException("Not a directory");
        }
        FileSystemDirectoryHandle.GetFileSystemHandleOptions opts = JSObjects.create();
        opts.setCreate(true);
        try {
            FileSystemFileHandle newHandle = dirHandle.getFileHandle(fileName, opts).await();
            return newHandle != null;
        } catch (Exception e) {
            // OPFS throws when an entry exists at this path with directory type.
            throw new IOException("Could not create file '" + fileName + "'", e);
        }
    }

    @Override
    public boolean createDirectory(String fileName) {
        System.out.println("OPFSVirtualFile.createDirectory: " + fileName);
        FileSystemDirectoryHandle dirHandle = getExistingDirectoryHandle();
        if (dirHandle == null) {
            return false;
        }
        FileSystemDirectoryHandle.GetFileSystemHandleOptions opts = JSObjects.create();
        opts.setCreate(true);
        try {
            FileSystemDirectoryHandle newHandle = dirHandle.getDirectoryHandle(fileName, opts).await();
            return newHandle != null;
        } catch (Exception e) {
            // OPFS throws when an entry exists at this path with file type.
            return false;
        }
    }

    @Override
    public boolean delete() {
        if (parentHandle == null || isRoot()) {
            return false; // Cannot delete root
        }
        FileSystemDirectoryHandle.RemoveEntryOptions opts = JSObjects.create();
        opts.setRecursive(true); // Ensure directories are deleted with their contents
        try {
            parentHandle.removeEntry(name, opts).await();
        } catch (Exception e) {
            return false; // Deletion failed (e.g. due to permissions)
        }
        return true;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        FileSystemDirectoryHandle destDir = getExistingDirectoryHandle();
        if (destDir == null) {
            return false;
        }
        OPFSVirtualFile source = (OPFSVirtualFile) file;
        FileSystemHandle sourceHandle = source.getExistingHandle();
        if (sourceHandle == null) {
            return false;
        }

        // 1. Try the efficient move first
        try {
            sourceHandle.move(destDir).await();
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
        if (isDirectory()) {
            // TODO: support metadata for files + directories to get real last modified time
            // for directories
            return 0;
        }
        try {
            return (long) getFile().getLastModified();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
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
