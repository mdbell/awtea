package me.mdbell.awtea.io.mount;

import org.teavm.runtime.fs.VirtualFileSystem;

class MountResolution {

    final String mountPath;
    final VirtualFileSystem fileSystem;
    final String relativePath;

    MountResolution(String mountPath, VirtualFileSystem fileSystem, String relativePath) {
        this.mountPath = mountPath;
        this.fileSystem = fileSystem;
        this.relativePath = relativePath;
    }
}
