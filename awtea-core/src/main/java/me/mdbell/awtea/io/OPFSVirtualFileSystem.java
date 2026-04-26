package me.mdbell.awtea.io;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.jso.FileSystemDirectoryHandle;
import me.mdbell.awtea.util.jso.StorageManager;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSObjects;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

@ExtensionMethod({JSObjectsExtensions.class})
public class OPFSVirtualFileSystem implements VirtualFileSystem {

    private static final StorageManager storage = getStorage();

    private FileSystemDirectoryHandle rootHandle;

    private String[] roots = new String[]{"/"};

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
        FileSystemDirectoryHandle.GetFileSystemHandleOptions options = JSObjects.create();
        options.setCreate(false);
        System.out.println("OPFSVirtualFileSystem.getFile: " + path);
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return OPFSVirtualFile.root(getRootHandle());
        }

        String[] parts = canonicalize(path).split("/");
        FileSystemDirectoryHandle currentDir = getRootHandle();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            boolean isLastPart = i == parts.length - 1;
            if (isLastPart) {
                return new OPFSVirtualFile(currentDir, part);
            }

            try {
                FileSystemDirectoryHandle nextDir = currentDir.getDirectoryHandle(part, options).await();
                if (nextDir == null) {
                    return null;
                }
                currentDir = nextDir;
            } catch (Exception e) {
                return null;
            }
        }

        return OPFSVirtualFile.root(currentDir);
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
