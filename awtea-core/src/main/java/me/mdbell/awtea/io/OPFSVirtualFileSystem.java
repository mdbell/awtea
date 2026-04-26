package me.mdbell.awtea.io;

import me.mdbell.awtea.util.jso.FileSystemDirectoryHandle;
import me.mdbell.awtea.util.jso.FileSystemFileHandle;
import me.mdbell.awtea.util.jso.StorageManager;
import org.teavm.jso.JSBody;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

public class OPFSVirtualFileSystem implements VirtualFileSystem {

    private static final StorageManager storage = getStorage();

    private FileSystemDirectoryHandle rootHandle;

    private String[] roots = new String[] { "/" };

    private FileSystemDirectoryHandle getRootHandle() {
        if (rootHandle == null) {
            rootHandle = storage.getDirectory().await();
        }
        return rootHandle;
    }

    @Override
    public String getUserDir() {
        return "/";
    }

    @Override
    public VirtualFile getFile(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return new OPFSVirtualFile(null, getRootHandle());
        }

        String[] parts = canonicalize(path).split("/");
        FileSystemDirectoryHandle currentDir = getRootHandle();
        FileSystemDirectoryHandle parentDir = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            boolean isLastPart = i == parts.length - 1;

            if (!isLastPart) {
                try {
                    FileSystemDirectoryHandle nextDir = currentDir.getDirectoryHandle(part).await();
                    if (nextDir == null) {
                        return null;
                    }
                    parentDir = currentDir;
                    currentDir = nextDir;
                    continue;
                } catch (Exception e) {
                    return null;
                }
            }

            try {
                FileSystemDirectoryHandle dirHandle = currentDir.getDirectoryHandle(part).await();
                if (dirHandle != null) {
                    return new OPFSVirtualFile(currentDir, dirHandle);
                }
            } catch (Exception ignored) {
                // Fall back to checking for a file with the same name.
            }

            try {
                FileSystemFileHandle fileHandle = currentDir.getFileHandle(part).await();
                if (fileHandle != null) {
                    return new OPFSVirtualFile(currentDir, fileHandle);
                }
            } catch (Exception ignored) {
                // File does not exist.
            }

            return null;
        }

        return new OPFSVirtualFile(parentDir, currentDir);
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        // strip out leading slash if exists, since OPFS doesn't use leading slash for
        // root directory
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    @Override
    public String[] getRoots() {
        return roots;
    }

    @JSBody(script = "return navigator.storage;")
    private static native StorageManager getStorage();
}
