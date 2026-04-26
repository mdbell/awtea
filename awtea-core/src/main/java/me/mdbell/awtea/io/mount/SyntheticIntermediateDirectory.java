package me.mdbell.awtea.io.mount;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic directory representing an intermediate path that is an ancestor
 * of one or more named mount points but is not itself a mount or a real file.
 */
class SyntheticIntermediateDirectory implements VirtualFile {

    private final MountVirtualFileSystem owner;
    private final String path;

    SyntheticIntermediateDirectory(MountVirtualFileSystem owner, String path) {
        this.owner = owner;
        this.path = path;
    }

    @Override
    public String getName() {
        return MountVirtualFileSystem.lastSegment(path);
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
        List<String> names = new ArrayList<>();
        // Merge base FS listing at this path (if base FS has the directory).
        if (owner.baseFileSystem != null) {
            VirtualFile baseDir = owner.baseFileSystem.getFile(path);
            if (baseDir != null && baseDir.isDirectory()) {
                String[] baseFiles = baseDir.listFiles();
                if (baseFiles != null) {
                    for (String f : baseFiles) {
                        names.add(f);
                    }
                }
            }
        }
        // Add next segment of child mounts (these shadow base entries).
        for (String segment : owner.listChildMountSegments(path)) {
            if (!names.contains(segment)) {
                names.add(segment);
            }
        }
        return names.toArray(new String[0]);
    }

    private VirtualFile baseDir() {
        if (owner.baseFileSystem == null) {
            return null;
        }
        VirtualFile f = owner.baseFileSystem.getFile(path);
        return (f != null && f.isDirectory()) ? f : null;
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        throw new UnsupportedOperationException("Cannot open accessor for intermediate directory: " + path);
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        VirtualFile base = baseDir();
        return base != null && base.createFile(fileName);
    }

    @Override
    public boolean createDirectory(String fileName) {
        VirtualFile base = baseDir();
        return base != null && base.createDirectory(fileName);
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        VirtualFile base = baseDir();
        return base != null && base.adopt(file, fileName);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        VirtualFile base = baseDir();
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
