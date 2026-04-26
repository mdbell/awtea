package me.mdbell.awtea.io;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MountVirtualFileSystem implements VirtualFileSystem {

    private static final String ROOT = "/";

    private final Map<String, VirtualFileSystem> mounts = new LinkedHashMap<>();
    private VirtualFileSystem baseFileSystem;

    public void mount(String mountPath, VirtualFileSystem fileSystem) {
        if (fileSystem == null) {
            throw new IllegalArgumentException("Mounted filesystem cannot be null");
        }

        String normalizedPath = canonicalize(mountPath);
        if (ROOT.equals(normalizedPath)) {
            baseFileSystem = fileSystem;
        } else {
            mounts.put(normalizedPath, fileSystem);
        }
    }

    @Override
    public String getUserDir() {
        return ROOT;
    }

    @Override
    public VirtualFile getFile(String path) {
        String normalizedPath = canonicalize(path);
        if (ROOT.equals(normalizedPath)) {
            return new MountRootVirtualFile();
        }

        String directMount = findDirectMount(normalizedPath);
        if (directMount != null) {
            return new MountPointVirtualFile(directMount, mounts.get(directMount));
        }

        MountResolution resolution = resolve(normalizedPath);
        if (resolution != null) {
            return resolution.fileSystem.getFile(resolution.relativePath);
        }

        // Fall through to base filesystem if no named mount matches.
        if (baseFileSystem != null) {
            return baseFileSystem.getFile(normalizedPath);
        }

        return null;
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        if (path == null || path.isEmpty()) {
            return ROOT;
        }

        String normalized = path.replace('\\', '/').replaceAll("/+", "/");
        if (!normalized.startsWith(ROOT)) {
            normalized = ROOT + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith(ROOT)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Override
    public String[] getRoots() {
        return new String[] { ROOT };
    }

    private String findDirectMount(String normalizedPath) {
        return mounts.containsKey(normalizedPath) ? normalizedPath : null;
    }

    private MountResolution resolve(String normalizedPath) {
        MountResolution bestMatch = null;
        for (Map.Entry<String, VirtualFileSystem> entry : mounts.entrySet()) {
            String mountPath = entry.getKey();
            if (!normalizedPath.equals(mountPath) && !normalizedPath.startsWith(mountPath + ROOT)) {
                continue;
            }

            if (bestMatch != null && mountPath.length() <= bestMatch.mountPath.length()) {
                continue;
            }

            String relativePath = normalizedPath.equals(mountPath)
                    ? entry.getValue().getUserDir()
                    : normalizedPath.substring(mountPath.length());
            if (relativePath == null || relativePath.isEmpty()) {
                relativePath = entry.getValue().getUserDir();
            }

            bestMatch = new MountResolution(mountPath, entry.getValue(), relativePath);
        }
        return bestMatch;
    }

    private String[] listRootFiles() {
        List<String> names = new ArrayList<>();
        // Include base filesystem root listing first.
        if (baseFileSystem != null) {
            VirtualFile baseRoot = baseFileSystem.getFile(baseFileSystem.getUserDir());
            if (baseRoot != null) {
                String[] baseFiles = baseRoot.listFiles();
                if (baseFiles != null) {
                    for (String f : baseFiles) {
                        names.add(f);
                    }
                }
            }
        }
        // Append named mount-point names (these shadow base entries with the same
        // name).
        for (String mountPath : mounts.keySet()) {
            String mountName = mountPath.substring(1);
            if (!names.contains(mountName)) {
                names.add(mountName);
            }
        }
        return names.toArray(new String[0]);
    }

    private static final class MountResolution {
        private final String mountPath;
        private final VirtualFileSystem fileSystem;
        private final String relativePath;

        private MountResolution(String mountPath, VirtualFileSystem fileSystem, String relativePath) {
            this.mountPath = mountPath;
            this.fileSystem = fileSystem;
            this.relativePath = relativePath;
        }
    }

    private final class MountRootVirtualFile implements VirtualFile {

        @Override
        public String getName() {
            return ROOT;
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
            if (baseFileSystem == null) {
                return null;
            }
            return baseFileSystem.getFile(baseFileSystem.getUserDir());
        }

        @Override
        public String[] listFiles() {
            return listRootFiles();
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

    private static final class MountPointVirtualFile implements VirtualFile {

        private final String mountPath;
        private final VirtualFile delegateRoot;

        private MountPointVirtualFile(String mountPath, VirtualFileSystem fileSystem) {
            this.mountPath = mountPath;
            this.delegateRoot = fileSystem.getFile(fileSystem.getUserDir());
        }

        @Override
        public String getName() {
            return mountPath.substring(1);
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
}