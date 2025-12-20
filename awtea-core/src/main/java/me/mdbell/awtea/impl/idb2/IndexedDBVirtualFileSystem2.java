package me.mdbell.awtea.impl.idb2;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

/**
 * Modern IndexedDB-backed Virtual File System using Blob storage.
 * Provides better random access performance than chunked storage.
 */
public class IndexedDBVirtualFileSystem2 implements VirtualFileSystem {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexedDBVirtualFileSystem2.class);
    
    public static final String ROOT_DIR = "/";
    public static final String HOME_DIR = "/home";
    
    public IndexedDBVirtualFileSystem2() {
        logger.info("Initializing IndexedDB VFS v2");
        
        // Ensure root directory exists
        initializeFilesystem();
        
        logger.info("IndexedDB VFS v2 initialized successfully");
    }
    
    private void initializeFilesystem() {
        // Check if root exists
        if (!IndexedDBHelper2.exists(ROOT_DIR)) {
            FileEntry root = FileEntry.createDirectory(ROOT_DIR);
            IndexedDBHelper2.putFileSync(root);
            logger.debug("Created root directory");
        }
        
        // Ensure home directory exists
        if (!IndexedDBHelper2.exists(HOME_DIR)) {
            FileEntry home = FileEntry.createDirectory(HOME_DIR);
            IndexedDBHelper2.putFileSync(home);
            logger.debug("Created home directory");
        }
    }
    
    @Override
    public String getUserDir() {
        return HOME_DIR;
    }
    
    @Override
    public VirtualFile getFile(String path) {
        if (path == null || path.isEmpty()) {
            path = ROOT_DIR;
        }
        
        // Normalize the path
        String normalizedPath = VFSPath.normalize(path);
        
        return new IndexedDBVirtualFile2(normalizedPath);
    }
    
    @Override
    public boolean isWindows() {
        return false; // Unix-style paths
    }
    
    @Override
    public String canonicalize(String path) {
        return VFSPath.normalize(path);
    }
    
    @Override
    public String[] getRoots() {
        return new String[]{ROOT_DIR};
    }
    
    /**
     * Get VFS statistics
     */
    public VFSStats getStats() {
        return IndexedDBHelper2.getStats();
    }
    
    /**
     * Print VFS statistics to logger
     */
    public void logStats() {
        VFSStats stats = getStats();
        logger.info("VFS Statistics: {}", stats);
    }
}
