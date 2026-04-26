package me.mdbell.awtea.io;

import me.mdbell.awtea.util.jso.FileSystemDirectoryHandle;
import me.mdbell.awtea.util.jso.StorageManager;
import org.teavm.jso.JSBody;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

public class OPFSVirtualFileSystem implements VirtualFileSystem {

    private static final StorageManager storage = getStorage();

    private FileSystemDirectoryHandle rootHandle;

    private String[] roots;

    private FileSystemDirectoryHandle getRootHandle() {
        if (rootHandle == null) {
            rootHandle = storage.getDirectory().await();
            roots = new String[]{rootHandle.getName()};
        }
        return rootHandle;
    }

    @Override
    public String getUserDir() {
        return getRootHandle().getName();
    }

    @Override
    public VirtualFile getFile(String path) {
        return null;
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        return "";
    }

    @Override
    public String[] getRoots() {
        return roots;
    }

    @JSBody(script = "return navigator.storage;")
    private static native StorageManager getStorage();
}
